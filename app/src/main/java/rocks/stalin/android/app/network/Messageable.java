package rocks.stalin.android.app.network;

import com.squareup.wire.Message;

/**
 * Created by delusional on 5/11/17.
 */

public interface Messageable<M extends Message<M, B>, B extends Message.Builder<M, B>> {
    M toMessage();
}
