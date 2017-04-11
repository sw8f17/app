package rocks.stalin.android.app.network;

import com.squareup.wire.Message;

import java.util.HashMap;
import java.util.Map;

import rocks.stalin.android.app.proto.Welcome;

public class MessageRegistry {
    private static MessageRegistry INSTANCE = null;

    public static MessageRegistry getInstance() {
        if(INSTANCE == null)
            INSTANCE = new MessageRegistry();
        return INSTANCE;
    }

    private static Map<Class<? extends Message>, Integer> messages = new HashMap<>();

    //Add messages here
    private MessageRegistry() {
        messages.put(Welcome.class, 1);
    }

    public int getID(Class<? extends Message> type) {
        return messages.get(type);
    }
}
