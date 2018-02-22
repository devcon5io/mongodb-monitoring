package io.devcon5.timeseries;

public interface Decoder<T> {

    Measurement decode(T encodedMeasurement);
}
