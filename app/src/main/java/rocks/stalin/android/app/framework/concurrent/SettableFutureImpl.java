package rocks.stalin.android.app.framework.concurrent;

import android.support.annotation.NonNull;
import android.support.v4.animation.ValueAnimatorCompat;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.telecom.Call;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rocks.stalin.android.app.framework.functional.Consumer;

//TODO: Better name
public class SettableFutureImpl<T> implements SettableFuture<T> {
    private T value;
    private Throwable exc;
    private ObservableFuture<T> future;

    private boolean canceled;

    private Lock lock = new ReentrantLock(true);
    private Condition condition = lock.newCondition();

    private Consumer<T> listener;
    private Consumer<Throwable> errorListener;

    public SettableFutureImpl() {
        value = null;
        exc = null;
        canceled = false;
    }

    @Override
    public void setListener(Consumer<T> listener) {
        setListener(listener, null);
    }

    @Override
    public void setListener(Consumer<T> listener, Consumer<Throwable> errorListener) {
        this.listener = listener;
        if(this.listener != null) {
            if(value != null)
                listener.call(value);
        }
        this.errorListener = errorListener;
        if(this.errorListener != null) {
            if(exc != null)
                errorListener.call(exc);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        lock.lock();
        if(isDone())
            return false;

        canceled = true;
        if(mayInterruptIfRunning) {
            exc = new CancellationException("Future was canceled");
            condition.signalAll();
        }
        lock.unlock();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public boolean isDone() {
        return value != null || exc != null;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        lock.lock();

        if(isDone())
            return getValue();

        condition.await();

        T maybeValue = getValue();
        lock.unlock();
        return maybeValue;
    }

    @Override
    public T get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        lock.lock();

        if(isDone())
            return getValue();

        condition.await(timeout, unit);

        T maybeValue = getValue();
        lock.unlock();
        return maybeValue;
    }

    @Override
    public void set(T value) {
        lock.lock();

        if(isDone())
            return;

        this.value = value;

        condition.signalAll();
        if(listener != null)
            listener.call(value);

        lock.unlock();
    }

    @Override
    public void setException(Throwable e) {
        lock.lock();

        if(isDone())
            return;

        this.exc = e;

        condition.signalAll();
        if(errorListener != null)
            errorListener.call(e);

        lock.unlock();
    }

    @Override
    public void setFuture(ObservableFuture<T> future) {
        lock.lock();

        if(isDone())
            return;

        this.future = future;

        condition.signalAll();

        future.setListener(new Consumer<T>() {
            @Override
            public void call(T value) {
                set(value);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void call(Throwable value) {
                setException(value);
            }
        });

        lock.unlock();
    }

    private T getValue() throws ExecutionException {
        maybeThrow();
        return value;
    }

    private void maybeThrow() throws ExecutionException {
        lock.lock();
        if(exc != null) {
            if(exc instanceof ExecutionException) {
                throw (ExecutionException)exc;
            } else if(exc instanceof RuntimeException) {
                throw (RuntimeException)exc;
            } else {
                exc = null; //Swallow the exception
            }
        }
        lock.unlock();
    }
}
