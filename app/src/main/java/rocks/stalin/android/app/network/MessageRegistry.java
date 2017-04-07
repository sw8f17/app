package rocks.stalin.android.app.network;

import android.content.IntentFilter;
import android.util.SparseArray;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;

import java.util.HashMap;
import java.util.Map;

import rocks.stalin.android.app.proto.Welcome;

/**
 * Created by delusional on 4/7/17.
 */

public class MessageRegistry {
    private static final MessageRegistry INSTANCE = new MessageRegistry();

    public static MessageRegistry getInstance() {
        return INSTANCE;
    }

    private static Map<Class<? extends Message>, Integer> messages = new HashMap();

    //Add messages here
    private MessageRegistry() {
        messages.put(Welcome.class, 1);
    }

    public int getID(Class<? extends Message> type) {
        return messages.get(type);
    }
}
