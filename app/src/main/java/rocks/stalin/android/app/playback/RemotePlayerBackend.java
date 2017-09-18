package rocks.stalin.android.app.playback;

import com.squareup.wire.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

import okio.ByteString;
import rocks.stalin.android.app.decoding.MediaInfo;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFutureTask;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.network.OffsetSource;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.proto.NewMusic;
import rocks.stalin.android.app.proto.Sync;
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

    @Override
    public void pushBuffer(ByteBuffer copy, Clock.Instant presentationOffset) {
        //TODO send the data
        Timestamp timestampMessage = new Timestamp.Builder()
                .millis(presentationOffset.getMillis())
                .nanos(presentationOffset.getNanos())
                .build();
        NewMusic musicMessage = new NewMusic.Builder()
                .data(ByteString.of(copy))
                .playtime(timestampMessage)
                .build();
        try {
            connection.send(musicMessage, NewMusic.class);
        } catch (IOException e) {
            LogHelper.w(TAG, "Failed transmitting the new music packet");
        }
    }

    @Override
    public void pushSync(Clock.Instant realTime, Clock.Instant mediaTime) {
        Timestamp realTimeMessage = new Timestamp.Builder()
                .millis(realTime.getMillis())
                .nanos(realTime.getNanos())
                .build();
        Timestamp mediaTimeMessage = new Timestamp.Builder()
                .millis(mediaTime.getMillis())
                .nanos(mediaTime.getNanos())
                .build();
        Sync syncMessage = new Sync.Builder()
                .realtime(realTimeMessage)
                .mediatime(mediaTimeMessage)
                .build();
        try {
            connection.send(syncMessage, Sync.class);
        } catch (IOException e) {
            LogHelper.w(TAG, "Failed transmitting the sync packet");
        }
    }

    @Override
    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void pushAction(final TimedAction<M, B> action) {
        ObservableFutureTask<Void> task = new ObservableFutureTask<>(new Runnable() {
            @Override
            public void run() {
                Message<M, B> actionMessage = action.toMessage();
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
