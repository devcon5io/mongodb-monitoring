package io.devcon5.timeseries;

public interface Encoder<T> {

    T encode(Measurement m);
}
