package rocks.stalin.android.app.network;

import com.squareup.wire.Message;

import java.util.HashMap;
import java.util.Map;

import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.PauseCommand;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.proto.SeekCommand;
import rocks.stalin.android.app.proto.SessionInfo;
import rocks.stalin.android.app.proto.SongChangeCommand;
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
        messages.put(Music.class, 2);
        messages.put(PlayCommand.class, 3);
        messages.put(PauseCommand.class, 4);
        messages.put(SeekCommand.class, 5);
        messages.put(SongChangeCommand.class, 6);
        messages.put(SessionInfo.class, 7);
    }

    public int getID(Class<? extends Message> type) {
        return messages.get(type);
    }
}
