package io.devcon5.timeseries;

import io.vertx.core.buffer.Buffer;

import java.util.HashMap;
import java.util.Map;

public class BufferEncoding implements Encoder<Buffer>, Decoder<Buffer> {

    private static final byte TYPE_INTEGER = (byte) 0x01;
    private static final byte TYPE_LONG = (byte) 0x02;
    private static final byte TYPE_FLOAT = (byte) 0x03;
    private static final byte TYPE_DOUBLE = (byte) 0x04;
    private static final byte TYPE_BOOLEAN = (byte) 0x05;
    private static final byte TYPE_STRING = (byte) 0x06;
    private static final byte ASSIGN = (byte) 0xfa;
    private static final byte SEPERATOR = (byte) 0xfd;
    private static final byte GROUP_SEPARATOR = (byte) 0xfe;

    @Override
    public Buffer encode(Measurement m) {
        final Buffer buf = Buffer.buffer(64);

        buf.appendString(m.name).appendByte(GROUP_SEPARATOR);
        buf.appendLong(m.timestamp).appendByte(GROUP_SEPARATOR);

        m.tags.forEach((k,v) -> buf.appendString(k).appendByte(ASSIGN).appendString(v).appendByte(SEPERATOR));
        buf.appendByte(GROUP_SEPARATOR);

        m.values.forEach((k,v) -> {
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

    @Override
    public Measurement decode(Buffer buf) {

        int start = 0, end;
        end = findNext(buf, start, GROUP_SEPARATOR);
        final String name = buf.getString(start, end);
        start = end + 1;

        final long timestamp = buf.getLong(start);
        start = start + 8 + 1;

        end = findNext(buf, start, GROUP_SEPARATOR);
        final Map<String,String> tags = parseTags(buf, start, end);
        start = end + 1;

        final Map<String,Object> values = parseValues(buf, start, buf.length());

        return new Measurement(name, timestamp, tags, values);
    }

    private Map<String, String> parseTags(Buffer buf, int start, int end) {

        Map<String,String> tags = new HashMap<>();

        String key, value;
        int from = start, to;

        while(from < end){
            to = findNext(buf, from, ASSIGN);
            key = buf.getString(from, to);
            from = to + 1;

            to = findNext(buf, from, SEPERATOR);
            value = buf.getString(from,to);
            from = to + 1;
            tags.put(key, value);
        }

        return tags;
    }

    private Map<String, Object> parseValues(Buffer buf, int start, int end) {

        final Map<String,Object> values = new HashMap<>();

        String key;
        Object value;
        int from = start, to;

        while(from < end){
            to = findNext(buf, from, ASSIGN);
            key = buf.getString(from, to);
            from = to + 1;

            to = findNext(buf, from, SEPERATOR);
            value = parseValue(buf, from, to);
            from = to + 1;
            values.put(key, value);
        }

        return values;
    }

    private Object parseValue(Buffer buf, int from, int to) {
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

    private int findNext(Buffer buf, int from, byte assign) {
        return checkDelimiter(findPos(buf, from, assign));
    }

    private int checkDelimiter(int delim) {
        if(delim == -1){
            throw new IllegalArgumentException("Buffer has invalid structure");
        }
        return delim;
    }

    private int findPos(Buffer buf, int start, byte search) {
        for(int i = start, len = buf.length(); i < len; i++){
            if(buf.getByte(i) == search){
                return i;
            }
        }
        return -1;
    }
}
