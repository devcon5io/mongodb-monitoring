package io.devcon5.collector;

import java.util.function.Function;

public interface Parser<T> extends Function<String,T> {

}
