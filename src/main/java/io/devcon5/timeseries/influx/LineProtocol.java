package io.devcon5.timeseries.influx;

import io.devcon5.timeseries.Measurement;
import io.vertx.core.buffer.Buffer;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class LineProtocol {

    public static Buffer toBuffer(Collection<Measurement> measures) {
        final int initialSize = 128 * measures.size();
        return measures.stream()
                       .collect(Collector.of(() -> Buffer.buffer(initialSize), LineProtocol::appendMeasurement, Buffer::appendBuffer));
    }

    public static Buffer toBuffer(Measurement... measures) {
        final int initialSize = 128 * measures.length;
        return Stream.of(measures)
                     .collect(Collector.of(() -> Buffer.buffer(initialSize), LineProtocol::appendMeasurement, Buffer::appendBuffer));
    }

    private static Buffer appendMeasurement(Buffer buf, Measurement m) {
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
