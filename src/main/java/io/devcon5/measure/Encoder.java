package io.devcon5.measure;

public interface Encoder<T> {

    T encode(Measurement m);
}
