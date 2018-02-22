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

    final long timestamp;
    final String name;
    final SortedMap<String, String> tags;
    final Map<String, Object> values;

    Measurement(String name, long ts, Map<String, String> tags, Map<String, Object> values) {
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

    public Buffer toBuffer() {

        return new BufferEncoding().encode(this);
    }

    public static Measurement fromJson(String jsonString) {
        return Json.decodeValue(jsonString, Measurement.class);
    }

    public static Measurement fromBuffer(Buffer buf) {
        return new BufferEncoding().decode(buf);
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
            if (timestamp == 0) {
                timestamp = System.currentTimeMillis() * 1_000_000;
            }
            if (numberValues.isEmpty() && booleanValues.isEmpty() && stringValues.isEmpty()) {
                throw new IllegalStateException("no values recorded");
            }

            return new Measurement(this);
        }

    }


}









