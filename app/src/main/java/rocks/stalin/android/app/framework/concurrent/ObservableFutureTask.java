package rocks.stalin.android.app.framework.concurrent;

import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import rocks.stalin.android.app.framework.functional.Consumer;

/**
 * Created by delusional on 5/14/17.
 */

public class ObservableFutureTask<T> extends FutureTask<T> implements ObservableFuture<T> {
    private Consumer<T> listener;
    private Consumer<Throwable> errorListener;

    public ObservableFutureTask(@NonNull Callable<T> callable) {
        super(callable);
    }

    public ObservableFutureTask(@NonNull Runnable runnable, T result) {
        super(runnable, result);
    }

    @Override
    public void setListener(Consumer<T> listener) {
        setListener(listener, null);
    }

    @Override
    public void setListener(Consumer<T> listener, Consumer<Throwable> errorListener) {
        this.listener = listener;
        this.errorListener = errorListener;
        if(isDone())
            done();
    }

    @Override
    protected void done() {
        final T value;
        try {
            value = getUninterruptibly();
        } catch (ExecutionException | RuntimeException e) {
            if(errorListener != null)
                errorListener.call(e);
            return;
        }
        if(listener != null)
            listener.call(value);
        super.done();
    }

    private T getUninterruptibly() throws ExecutionException {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return get();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
