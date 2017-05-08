package rocks.stalin.android.app.playback;

import java.io.IOException;
import java.nio.ByteBuffer;

import okio.ByteString;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/8/17.
 */

class RemoteMixer implements AudioMixer {
    private static final String TAG = LogHelper.makeLogTag(RemoteMixer.class);
    private MessageConnection connection;

    public RemoteMixer(MessageConnection connection) {

        this.connection = connection;
    }

    @Override
    public void pushFrame(Clock.Instant nextTime, ByteBuffer read) {
        Timestamp timestampMessage = new Timestamp.Builder()
                .millis(nextTime.getMillis())
                .nanos(nextTime.getNanos())
                .build();
        Music musicMessage = new Music.Builder()
                .data(ByteString.of(read.array(), read.position(), read.limit()))
                .playtime(timestampMessage)
                .build();
        try {
            connection.send(musicMessage, Music.class);
        } catch (IOException e) {
            LogHelper.w(TAG, "Failed transmitting the package");
        }
    }

    @Override
    public void pushAction(TimedAction action) {
    }
}
