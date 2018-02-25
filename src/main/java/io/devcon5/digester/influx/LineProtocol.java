package io.devcon5.digester.influx;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;

import io.devcon5.measure.Encoder;
import io.devcon5.measure.Measurement;
import io.vertx.core.buffer.Buffer;

public class LineProtocol {

    public static Encoder<Buffer> encoder() {

        return new LineProtocolEncoder();
    }

    private static class LineProtocolEncoder implements Encoder<Buffer> {

        @Override
        public Buffer encode(final Collection<Measurement> measurements) {

            final int initialSize = 128 * measurements.size();
            return measurements.stream()
                           .collect(Collector.of(() -> Buffer.buffer(initialSize),
                                                 this::appendMeasurement,
                                                 Buffer::appendBuffer));
        }

        private Buffer appendMeasurement(Buffer buf, Measurement m) {

            buf.appendString(m.getName());
            m.getTags().forEach((k, v) -> buf.appendString(",").appendString(k).appendString("=").appendString(v));
            buf.appendString(" ");

            final AtomicBoolean firstValue = new AtomicBoolean(true);
            m.getValues().forEach((k, v) -> {
                if (!firstValue.compareAndSet(true, false)) {
                    buf.appendString(",");
                }
                buf.appendString(k).appendString("=").appendString(String.valueOf(v));
                if (v instanceof Integer || v instanceof Long) {
                    buf.appendString("i");
                }
            });

            buf.appendString(String.valueOf(m.getTimestamp()));
            buf.appendString("\n");
            return buf;
        }
    }

}
