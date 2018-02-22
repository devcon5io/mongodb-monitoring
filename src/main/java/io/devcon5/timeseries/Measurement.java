package io.devcon5.timeseries;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class Measurement {

    private static final byte TYPE_INTEGER = (byte) 0x01;
    private static final byte TYPE_LONG = (byte) 0x02;
    private static final byte TYPE_FLOAT = (byte) 0x03;
    private static final byte TYPE_DOUBLE = (byte) 0x04;
    private static final byte TYPE_BOOLEAN = (byte) 0x05;
    private static final byte TYPE_STRING = (byte) 0x06;
    private static final byte ASSIGN = (byte) 0xfa;
    private static final byte SEPERATOR = (byte) 0xfd;
    private static final byte GROUP_SEPARATOR = (byte) 0xfe;

    private final long timestamp;
    private final String name;
    private final SortedMap<String, String> tags;
    private final Map<String, Object> values;

    Measurement(String name, long ts, Map<String,String> tags, Map<String,Object> values) {
        this.name = name;
        this.timestamp = ts;
        this.tags = Collections.unmodifiableSortedMap(new TreeMap<>(tags));
        this.values = Collections.unmodifiableMap(values);
    }

    Measurement(Measurement.Builder builder) {
        this.timestamp = builder.timestamp;
        this.name = builder.name;
        this.tags = Collections.unmodifiableSortedMap(new TreeMap<>(builder.tags));

        final Map<String, Object> values = new HashMap<>();
        values.putAll(builder.booleanValues);
        values.putAll(builder.numberValues);
        values.putAll(builder.stringValues);
        this.values = Collections.unmodifiableMap(values);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public SortedMap<String, String> getTags() {
        return tags;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public String toJson() {
        return Json.encode(this);
    }
    public Buffer toBuffer(){

        return new Encoder(this).encodeToBuffer();
    }

    public static Measurement fromJson(String jsonString){
        return Json.decodeValue(jsonString, Measurement.class);
    }

    public static Measurement fromBuffer(Buffer buf){
        return new Decoder(buf).decode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Measurement{");
        sb.append("name='").append(name).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", tags=").append(tags);
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private long timestamp;
        private Map<String, String> tags = new HashMap<>();

        private Map<String, Number> numberValues = new HashMap<>();
        private Map<String, Boolean> booleanValues = new HashMap<>();
        private Map<String, String> stringValues = new HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder timestamp(long nanos) {
            this.timestamp = nanos;
            return this;
        }

        public Builder tag(String name, String value) {
            this.tags.put(name, value);
            return this;
        }

        public Builder value(String name, String value) {
            this.stringValues.put(name, value);
            return this;
        }

        public Builder value(String name, Boolean value) {
            this.booleanValues.put(name, value);
            return this;
        }

        public Builder value(String name, Integer value) {
            this.numberValues.put(name, value);
            return this;
        }

        public Builder value(String name, Long value) {
            this.numberValues.put(name, value);
            return this;
        }

        public Builder value(String name, Double value) {
            this.numberValues.put(name, value);
            return this;
        }

        public Builder value(String name, Float value) {
            this.numberValues.put(name, value);
            return this;
        }

        public Measurement build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("name is not set");
            }
            if (timestamp < 0) {
                throw new IllegalStateException("timestamp is invalid: " + timestamp);
            }
            if(timestamp == 0){
                timestamp = System.currentTimeMillis() * 1_000_000;
            }
            if (numberValues.isEmpty() && booleanValues.isEmpty() && stringValues.isEmpty()) {
                throw new IllegalStateException("no values recorded");
            }

            return new Measurement(this);
        }

    }

    public static class Encoder {

        private final Measurement measurement;

        public Encoder(Measurement measurement) {
            this.measurement = measurement;
        }

        public Buffer encodeToBuffer() {
            final Buffer buf = Buffer.buffer(64);

            buf.appendString(measurement.name).appendByte(GROUP_SEPARATOR);
            buf.appendLong(measurement.timestamp).appendByte(GROUP_SEPARATOR);

            measurement.tags.forEach((k,v) -> buf.appendString(k).appendByte(ASSIGN).appendString(v).appendByte(SEPERATOR));
            buf.appendByte(GROUP_SEPARATOR);

            measurement.values.forEach((k,v) -> {
                buf.appendString(k).appendByte(ASSIGN);
                appendValue(buf, v);
                buf.appendByte(SEPERATOR);
            });

            return buf;
        }

        private void appendValue(Buffer buf, Object v) {
            if(v instanceof Integer){
                buf.appendByte(TYPE_INTEGER).appendInt((Integer)v);
            } else
            if(v instanceof Long){
                buf.appendByte(TYPE_LONG).appendLong((Long)v);
            } else
            if(v instanceof Float){
                buf.appendByte(TYPE_FLOAT).appendFloat((Float)v);
            }
            if(v instanceof Double){
                buf.appendByte(TYPE_DOUBLE).appendDouble((Double)v);
            }
            if(v instanceof Boolean){
                buf.appendByte(TYPE_BOOLEAN).appendByte((byte) ((Boolean) v ? 1 : 0));
            }
            if(v instanceof String){
                buf.appendByte(TYPE_STRING).appendString((String)v);
            }
        }

    }
    public static class Decoder {

        private final Buffer buf;

        public Decoder(Buffer buf) {
            this.buf = buf;
        }

        public Measurement decode() {

            int start = 0, end;
            end = findNext(start, GROUP_SEPARATOR);
            final String name = buf.getString(start, end);
            start = end + 1;

            final long timestamp = buf.getLong(start);
            start = start + 8 + 1;

            end = findNext(start, GROUP_SEPARATOR);
            final Map<String,String> tags = parseTags(start, end);
            start = end + 1;

            final Map<String,Object> values = parseValues(start, buf.length());

            return new Measurement(name, timestamp, tags, values);
        }

        private Map<String, String> parseTags(int start, int end) {

            Map<String,String> tags = new HashMap<>();

            String key, value;
            int from = start, to;

            while(from < end){
                to = findNext(from, ASSIGN);
                key = buf.getString(from, to);
                from = to + 1;

                to = findNext(from, SEPERATOR);
                value = buf.getString(from,to);
                from = to + 1;
                tags.put(key, value);
            }

            return tags;
        }

        private Map<String, Object> parseValues(int start, int end) {

            final Map<String,Object> values = new HashMap<>();

            String key;
            Object value;
            int from = start, to;

            while(from < end){
                to = findNext(from, ASSIGN);
                key = buf.getString(from, to);
                from = to + 1;

                to = findNext(from, SEPERATOR);
                value = parseValue(from, to);
                from = to + 1;
                values.put(key, value);
            }

            return values;
        }

        private Object parseValue(int from, int to) {
            switch(buf.getByte(from)){
                case TYPE_BOOLEAN:
                    return buf.getByte(from + 1) == 1;
                case TYPE_INTEGER:
                    return buf.getInt(from + 1);
                case TYPE_LONG:
                    return buf.getLong(from + 1);
                case TYPE_FLOAT:
                    return buf.getFloat(from + 1);
                case TYPE_DOUBLE:
                    return buf.getDouble(from + 1);
                case TYPE_STRING:
                    return buf.getString(from + 1, to);
                default:
                    throw new IllegalArgumentException("Invalid type indicator: " + buf.getByte(from));
            }
        }

        private int findNext(int from, byte assign) {
            return checkDelimiter(findPos(from, assign));
        }

        private int checkDelimiter(int delim) {
            if(delim == -1){
                throw new IllegalArgumentException("Buffer has invalid structure");
            }
            return delim;
        }

        private int findPos(int start, byte search) {
            for(int i = start, len = buf.length(); i < len; i++){
                if(buf.getByte(i) == search){
                    return i;
                }
            }
            return -1;
        }
    }

}









