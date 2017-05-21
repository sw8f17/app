package rocks.stalin.android.app.playback;

import com.squareup.wire.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

import okio.ByteString;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFutureTask;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.network.OffsetSource;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

class RemotePlayerBackend implements TimedEventQueue {
    private static final String TAG = LogHelper.makeLogTag(RemotePlayerBackend.class);

    private final MessageConnection connection;
    private final OffsetSource timeProvider;
    private TaskExecutor executor;

    public RemotePlayerBackend(MessageConnection connection, OffsetSource timeProvider, TaskExecutor executor) {
        this.connection = connection;
        this.timeProvider = timeProvider;
        this.executor = executor;
    }

    public void pushFrame(MP3MediaInfo mediaInfo, Clock.Instant nextTime, ByteBuffer read) {
        Clock.Instant correctedNextTime = nextTime.add(timeProvider.getOffset());
        LogHelper.i(TAG, "Corrected time ", nextTime, " by ", timeProvider.getOffset(), " to ", correctedNextTime);
        Timestamp timestampMessage = new Timestamp.Builder()
                .millis(correctedNextTime.getMillis())
                .nanos(correctedNextTime.getNanos())
                .build();
        Music musicMessage = new Music.Builder()
                .data(ByteString.of(read))
                .playtime(timestampMessage)
                .build();
        try {
            connection.send(musicMessage, Music.class);
        } catch (IOException e) {
            LogHelper.w(TAG, "Failed transmitting the music packet");
        }
    }

    @Override
    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void pushAction(final TimedAction<M, B> action) {
        ObservableFutureTask<Void> task = new ObservableFutureTask<>(new Runnable() {
            @Override
            public void run() {
                final Message<M, B> actionMessage = action.toMessage();
                try {
                    connection.send(actionMessage, actionMessage.getClass());
                } catch (IOException e) {
                    //TODO: Handle error
                    LogHelper.e(TAG, "Failed pushing action to a client");
                    e.printStackTrace();
                }
            }
        }, null);
        executor.submit(task);
    }

    @Override
    public void release() {

    }
}
