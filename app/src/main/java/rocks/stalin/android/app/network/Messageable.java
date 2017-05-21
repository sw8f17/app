package rocks.stalin.android.app.network;

import com.squareup.wire.Message;

import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;

/**
 * Created by delusional on 5/11/17.
 */

public interface Messageable<M extends Message<M, B>, B extends Message.Builder<M, B>> {
    Message<M, B> toMessage();
}
