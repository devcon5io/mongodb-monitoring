package io.devcon5.collector;

import io.vertx.core.AsyncResult;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Helper class that allows to reuse behavior for dealing with an {@link AsyncResult}<br>
 * Example:
 *
 * <pre><code>
 *     ResultHandler.create(MyResult.class)
 *                  .onSuccess(result -> vertx.eventBus().publish("someAddr", result))
 *                  .orElse(t -> LOG.error("Error occured", t))
 *                  .apply(myAsyncResult);
 * </code></pre>
 *
 * @param <T>
 *      the type of the actual result wrapped by the AsyncResult
 */
public class ResultHandler<T> implements Consumer<AsyncResult<T>> {

    public static <T> ResultHandler<T> create(Class<T> __) {
        return create();
    }

    public static <T> ResultHandler<T> create() {
        return new ResultHandler<>();
    }

    private Function<AsyncResult<T>, Boolean> successHandler;
    private Function<AsyncResult<T>, Boolean> errorHandler;

    @Override
    public void accept(AsyncResult<T> result) {
        if (!successHandler.apply(result)) {
            errorHandler.apply(result);
        }
    }

    public ResultHandler<T> onSuccess(Consumer<T> successHandler) {
        this.successHandler = result -> {
            if (result.succeeded()) {
                successHandler.accept(result.result());
                return true;
            }
            return false;
        };
        return this;
    }

    public ResultHandler<T> orElse(Consumer<Throwable> errorHandler) {
        this.errorHandler = result -> {
            if (result.failed()) {
                errorHandler.accept(result.cause());
                return true;
            }
            return false;
        };
        return this;
    }

}
