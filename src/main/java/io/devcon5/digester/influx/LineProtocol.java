/*
 *     Universal Collector for Metrics
 *     Copyright (C) 2017-2018 DevCon5 GmbH, Switzerland
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            buf.appendString(" ");
            buf.appendString(String.valueOf(m.getTimestamp()));
            buf.appendString("\n");
            return buf;
        }
    }

}
