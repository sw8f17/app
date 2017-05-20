package rocks.stalin.android.app.network;

import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;

public interface OffsetSourceFactory {
    ObservableFuture<? extends OffsetSource> create(final MessageConnection connection);
}
