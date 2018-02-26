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

/**
 * Decodes a single Measument Point from an encoded representation
 * @param <T>
 *   the type of the encoded measurement
 */
public interface Decoder<T> {

    /**
     * Decodes a measurement from an encoded representation.
     * The method always returns a measurement. In case the encoded artifact is not decodeable,
     * the method should throw an Exception
     *
     * @param encodedMeasurement
     *  the encoded measurement. Must not be null.
     * @return
     *  the decoded measurement. Must not be null.
     */
    Measurement[] decode(T encodedMeasurement);
}
