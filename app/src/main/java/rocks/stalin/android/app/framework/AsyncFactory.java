package rocks.stalin.android.app.framework;

import java.util.concurrent.Future;

import rocks.stalin.android.app.framework.concurrent.ObservableFuture;

/**
 * Created by delusional on 5/14/17.
 */

public interface AsyncFactory<T> {
    ObservableFuture<T> create();
}
