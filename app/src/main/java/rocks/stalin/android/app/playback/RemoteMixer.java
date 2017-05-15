package rocks.stalin.android.app.playback;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

import okio.ByteString;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.network.Messageable;
import rocks.stalin.android.app.network.OffsetSource;
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

    MixerHandler thread;
    private OffsetSource timeService;

    public RemoteMixer(MessageConnection connection, OffsetSource timeService) {
        this.connection = connection;
        thread = new MixerHandler(connection);
        this.timeService = timeService;
        thread.start();
    }

    @Override
    public void pushFrame(MP3MediaInfo mediaInfo, Clock.Instant nextTime, ByteBuffer read) {
        Clock.Instant correctedNextTime = nextTime.add(timeService.getOffset());
        LogHelper.i(TAG, "Corrected time ", nextTime, " by ", timeService.getOffset(), " to ", correctedNextTime);
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
    public void pushAction(TimedAction action) {
        Message message = thread.handler.obtainMessage(MixerHandler.WHAT_PUSH_ACTION, action);
        thread.handler.sendMessage(message);
    }

    //TODO: This doesn't make any sense, split the interfaces!!!
    @Override
    public TimedAction readAction() {
        throw new NoSuchMethodError("You can't call this on a fucking remote mixer you retard");
    }

    @Override
    public void flush() {
        throw new NoSuchMethodError("You can't call this on a fucking remote mixer you retard");
    }

    @Override
    public void change(MP3MediaInfo mediaInfo) {
        throw new NoSuchMethodError("You can't call this on a fucking remote mixer you retard");
    }

    private static class MixerHandler<M extends com.squareup.wire.Message<M, B>, B extends com.squareup.wire.Message.Builder<M, B>> extends HandlerThread {
        public static int WHAT_PUSH_ACTION = 1;

        public Handler handler;
        private MessageConnection connection;

        private MixerHandler(MessageConnection connection) {
            super("Remote Mixer thread X");
            this.connection = connection;
        }

        @Override
        protected void onLooperPrepared() {
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if(msg.what == WHAT_PUSH_ACTION) {
                        Messageable<M, B> action = (Messageable<M, B>) msg.obj;

                        final M actionMessage = action.toMessage();

                        try {
                            connection.send(actionMessage, actionMessage.getClass());
                        } catch (IOException e) {
                            LogHelper.w(TAG, "Failed transmitting the action");
                        }
                    }
                }
            };
        }
    }
}
