package io.devcon5.measure;

import java.util.Arrays;
import java.util.Collection;

public interface Encoder<T> {

    default T encode(Measurement... m) {
        return encode(Arrays.asList(m));
    }

    T encode(Collection<Measurement> m) ;
}
