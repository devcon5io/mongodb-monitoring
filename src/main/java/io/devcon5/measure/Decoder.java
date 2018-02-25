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
