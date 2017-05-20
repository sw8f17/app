package rocks.stalin.android.app.framework.concurrent.observable;

import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rocks.stalin.android.app.framework.functional.Consumer;
import rocks.stalin.android.app.network.MessageConnection;

/**
 * A super handy class to create futures which can fire callbacks on completion
 * @param <T> The result of the future
 */
public interface ObservableFuture<T> extends Future<T> {
    /**
     * Set the callback to fire when the future completes.
     *
     * <p>
     *     This variation will ignore all errors
     * </p>
     * @param listener The callback
     */
    void setListener(Consumer<T> listener);

    /**
     * Set the callback to fire when the future completes.
     *
     * <p>
     *     This variation includes support for error handling. Only one of the callbacks
     *     will ever be fired
     * </p>
     * @param listener The callback
     * @param errorListener The callback in case of error
     */
    void setListener(Consumer<T> listener, Consumer<Throwable> errorListener);
}
