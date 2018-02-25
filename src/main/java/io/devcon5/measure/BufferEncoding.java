package io.devcon5.measure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.vertx.core.buffer.Buffer;

/**
 * Encoding do write and read a measurement from a Vert.x buffer. Unlike the JsonFormat, the buffer encoding
 * is more efficient toward space requirement.
 */
public class BufferEncoding {

    private static final byte TYPE_INTEGER = (byte) 0x01;
    private static final byte TYPE_LONG = (byte) 0x02;
    private static final byte TYPE_FLOAT = (byte) 0x03;
    private static final byte TYPE_DOUBLE = (byte) 0x04;
    private static final byte TYPE_BOOLEAN = (byte) 0x05;
    private static final byte TYPE_STRING = (byte) 0x06;
    private static final byte LEADIN = (byte) 0x00;
    private static final byte ASSIGN = (byte) 0xfa;
    private static final byte SEPARATOR = (byte) 0xfd;
    private static final byte GROUP_SEPARATOR = (byte) 0xfe;

    public static Encoder<Buffer> encoder() {

        return new BufferEncoder();
    }

    public static Decoder<Buffer> decoder() {

        return new BufferDecoder();
    }

    private static class BufferEncoder implements Encoder<Buffer> {

        @Override
        public Buffer encode(Measurement... measurements) {

            final Buffer buf = Buffer.buffer(64 * measurements.length);

            for (Measurement m : measurements) {
                writeLeadIn(buf);
                writeName(buf, m);
                writeTimestamp(buf, m);
                writeTags(buf, m);
                writeValues(buf, m);
            }
            return buf;
        }

        private void writeLeadIn(final Buffer buf) {

            buf.appendByte(LEADIN);
        }

        private void writeName(final Buffer buf, final Measurement m) {

            buf.appendString(m.name)
               .appendByte(GROUP_SEPARATOR);
        }

        private void writeTimestamp(final Buffer buf, final Measurement m) {

            buf.appendLong(m.timestamp)
               .appendByte(GROUP_SEPARATOR);
        }

        private void writeTags(final Buffer buf, final Measurement m) {

            m.tags.forEach((k, v) -> buf.appendString(k)
                                        .appendByte(ASSIGN)
                                        .appendString(v)
                                        .appendByte(SEPARATOR));
            buf.appendByte(GROUP_SEPARATOR);
        }

        private void writeValues(final Buffer buf, final Measurement m) {

            m.values.forEach((k, v) -> {
                buf.appendString(k)
                   .appendByte(ASSIGN);
                writeValue(buf, v);
                buf.appendByte(SEPARATOR);
            });
        }

        private void writeValue(Buffer buf, Object v) {

            if (v instanceof Integer) {
                buf.appendByte(TYPE_INTEGER).appendInt((Integer) v);
            } else if (v instanceof Long) {
                buf.appendByte(TYPE_LONG).appendLong((Long) v);
            } else if (v instanceof Float) {
                buf.appendByte(TYPE_FLOAT).appendFloat((Float) v);
            } else if (v instanceof Double) {
                buf.appendByte(TYPE_DOUBLE).appendDouble((Double) v);
            } else if (v instanceof Boolean) {
                buf.appendByte(TYPE_BOOLEAN).appendByte((byte) ((Boolean) v ? 1 : 0));
            } else if (v instanceof String) {
                buf.appendByte(TYPE_STRING).appendString((String) v);
            }
        }
    }

    private static class BufferDecoder implements Decoder<Buffer> {

        @Override
        public Measurement[] decode(Buffer buf) {

            final List<Measurement> measurements = new ArrayList<>();

            int start = 0;
            while (start < buf.length()) {
                start = parseAndAddMeasurement(buf, start, measurements::add);
            }

            return measurements.toArray(new Measurement[0]);
        }

        private int parseAndAddMeasurement(final Buffer buf, int start, final Consumer<Measurement> callback) {

            final Measurement.Builder builder = Measurement.builder();

            start = findNext(buf, start, LEADIN) + 1;
            start = parseName(buf, start, builder::name);
            start = parseTimestamp(buf, start, builder::timestamp);
            start = parseTags(buf, start, builder::tag);
            start = parseValues(buf, start, builder::value);

            callback.accept(builder.build());

            return start;
        }

        private int parseName(final Buffer buf, int start, final Consumer<String> nameCallback) {

            final int end = findNext(buf, start, GROUP_SEPARATOR);
            nameCallback.accept(buf.getString(start, end));
            return end + 1;
        }

        private int parseTimestamp(final Buffer buf, int start, final Consumer<Long> timestampCallback) {

            timestampCallback.accept(buf.getLong(start));
            return start + 8 + 1;
        }

        private int parseTags(Buffer buf, int start, final BiConsumer<String,String> tagCallback) {

            String key, value;

            int end = findNext(buf, start, GROUP_SEPARATOR);
            int from = start, to;

            while (from < end) {
                to = findNext(buf, from, ASSIGN);
                key = buf.getString(from, to);
                from = to + 1;

                to = findNext(buf, from, SEPARATOR);
                value = buf.getString(from, to);
                from = to + 1;

                tagCallback.accept(key, value);
            }

            return from + 1;
        }

        private int parseValues(Buffer buf, int start, BiConsumer<String, Object> valueCallback) {

            int from = start;
            int to;

            //parse to the end of the buffer at maximum or to the next lead-in
            while (from < buf.length() && buf.getByte(from) != LEADIN) {

                to = findNext(buf, from, ASSIGN);

                final String key = buf.getString(from, to);

                from = parseAndAddValue(buf, to + 1, v -> valueCallback.accept(key, v));
            }

            return from;
        }

        private int parseAndAddValue(Buffer buf, int from, Consumer<Object> valueCallback) {

            switch (buf.getByte(from)) {
                case TYPE_BOOLEAN:
                    valueCallback.accept(buf.getByte(from + 1) == 1);
                    return from + 1 + 1 + 1;
                case TYPE_INTEGER:
                    valueCallback.accept(buf.getInt(from + 1));
                    return from + 1 + 4 + 1;
                case TYPE_LONG:
                    valueCallback.accept(buf.getLong(from + 1));
                    return from + 1 + 8 + 1;
                case TYPE_FLOAT:
                    valueCallback.accept(buf.getFloat(from + 1));
                    return from + 1 + 4 + 1;
                case TYPE_DOUBLE:
                    valueCallback.accept(buf.getDouble(from + 1));
                    return from + 1 + 8 + 1;
                case TYPE_STRING:
                    int end = findNext(buf, from + 1, SEPARATOR);
                    valueCallback.accept(buf.getString(from + 1, end));
                    return end + 1;
                default:
                    throw new IllegalArgumentException("Invalid type indicator: " + buf.getByte(from));
            }
        }

        private int findNext(Buffer buf, int from, byte assign) {

            return checkDelimiter(findPos(buf, from, assign));
        }

        private int checkDelimiter(int delim) {

            if (delim == -1) {
                throw new IllegalArgumentException("Buffer has invalid structure");
            }
            return delim;
        }

        private int findPos(Buffer buf, int start, byte search) {

            for (int i = start, len = buf.length(); i < len; i++) {
                if (buf.getByte(i) == search) {
                    return i;
                }
            }
            return -1;
        }
    }
}
