package rocks.stalin.android.app.framework;

import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;

/**
 * Created by delusional on 5/14/17.
 */

public interface AsyncFactory<T> {
    ObservableFuture<T> create();
}
