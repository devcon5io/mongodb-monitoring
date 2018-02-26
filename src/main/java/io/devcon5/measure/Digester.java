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

package io.devcon5.measure;

import java.util.Objects;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

/**
 * Marker interface for Digesters. Digesters should be implemented as Verticles that receive measurements sent
 * via Vert.x eventbus on the addess specified as DIGEST_ADDR
 */
public interface Digester {

    /**
     * The address digesters receive measurements.
     */
    String DIGEST_ADDR = "persist";

    /**
     * Decodes an encoded measure using an appropriate decoder.
     * The default implementation supports the following types:
     * <ul>
     * <li>{@link io.vertx.core.buffer.Buffer} - {@link io.devcon5.measure.BinaryEncoding}</li>
     * <li>{@link io.vertx.core.json.JsonArray - {@link io.devcon5.measure.JsonEncoding}}</li>
     * </ul>
     *
     * @param o
     *         the measurements in a specific encoding. Must not be null.
     *
     * @return the decoded measurements
     *
     * @throws java.lang.IllegalArgumentException
     *         if the object is neither of type Buffer nor JsonObject
     */
    default Measurement[] decode(Object o) {

        Objects.requireNonNull(o, "encoded measurement was null");

        if (o instanceof Buffer) {
            return BinaryEncoding.decoder().decode((Buffer) o);
        } else if (o instanceof JsonArray) {
            return JsonEncoding.decoder().decode((JsonArray) o);
        }
        throw new IllegalArgumentException("Unsupported type " + o.getClass());
    }
}
