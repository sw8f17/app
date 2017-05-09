package rocks.stalin.android.app.playback;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

import okio.ByteString;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.playback.actions.PlayAction;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.PlayCommand;
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

    public RemoteMixer(MessageConnection connection) {
        this.connection = connection;
        thread = new MixerHandler(connection);
        thread.start();
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
            LogHelper.w(TAG, "Failed transmitting the music packet");
        }
    }

    @Override
    public void pushAction(TimedAction action) {
        Message message = thread.handler.obtainMessage(MixerHandler.WHAT_PUSH_ACTION, action);
        thread.handler.sendMessage(message);
    }

    private static class MixerHandler extends HandlerThread {
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
                        TimedAction action = (TimedAction) msg.obj;

                        Timestamp timestampMessage = new Timestamp.Builder()
                                .millis(action.getTime().getMillis())
                                .nanos(action.getTime().getNanos())
                                .build();

                        final PlayCommand actionMessage = new PlayCommand.Builder()
                                .playtime(timestampMessage)
                                .build();

                        try {
                            connection.send(actionMessage, PlayCommand.class);
                        } catch (IOException e) {
                            LogHelper.w(TAG, "Failed transmitting the action");
                        }
                    }
                }
            };
        }
    }
}
