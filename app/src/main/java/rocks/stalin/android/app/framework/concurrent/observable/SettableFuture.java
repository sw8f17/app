package rocks.stalin.android.app.framework.concurrent.observable;

/**
 * Created by delusional on 5/14/17.
 */

public interface SettableFuture<T> extends ObservableFuture<T> {
    void set(T value);

    void setException(Throwable e);

    void setFuture(ObservableFuture<T> future);
}
