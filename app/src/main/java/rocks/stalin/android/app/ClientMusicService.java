package rocks.stalin.android.app;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;

import rocks.stalin.android.app.decoding.MP3Encoding;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.network.WifiP2PMessageClient;
import rocks.stalin.android.app.playback.LocalAudioMixer;
import rocks.stalin.android.app.playback.LocalSoundSink;
import rocks.stalin.android.app.playback.actions.MediaChangeAction;
import rocks.stalin.android.app.playback.actions.PauseAction;
import rocks.stalin.android.app.playback.actions.PlayAction;
import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.PauseCommand;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.NetworkHelper;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 4/6/17.
 */

public class ClientMusicService extends Service {
    private static final String TAG = LogHelper.makeLogTag(ClientMusicService.class);

    public static final String ACTION_CONNECT = "rocks.stalin.android.app.ACTION_CONNECT";
    public static final String ACTION_STOP = "rocks.stalin.android.app.ACTION_STOP";

    public static final String CONNECT_HOST_NAME = "CONNECT_HOST_NAME";
    public static final String CONNECT_PORT_NAME = "CONNECT_PORT_NAME";

    private WifiP2PMessageClient client;
    private MessageConnection connection = null;
    private LocalAudioMixer localAudioMixer;
    private LocalSoundSink sink;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        WifiP2pManager manager = getSystemService(WifiP2pManager.class);
        client = new WifiP2PMessageClient(manager);
        client.initialize(this);

        localAudioMixer = new LocalAudioMixer();
        sink = new LocalSoundSink(localAudioMixer);
        sink.initialize();
        localAudioMixer.pushAction(new MediaChangeAction(Clock.getTime(), new MP3MediaInfo(44100, 1, 0, MP3Encoding.UNSIGNED16)));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_CONNECT)) {
            LogHelper.v(TAG, "Connecting to server");

            String hostname = intent.getStringExtra(CONNECT_HOST_NAME);
            int port = intent.getIntExtra(CONNECT_PORT_NAME, -1);


            client.connect(this, hostname, port, new WifiP2PMessageClient.ConnectionListener() {
                @Override
                public void onConnected(MessageConnection connection) {
                    connection.addHandler(Welcome.class, new MessageConnection.MessageListener<Welcome, Welcome.Builder>() {
                        @Override
                        public void packetReceived(Welcome message) {
                            LogHelper.e(TAG, "DATA: ", message.song_name);
                        }
                    });
                    connection.addHandler(PlayCommand.class, new MessageConnection.MessageListener<PlayCommand, PlayCommand.Builder>() {
                        @Override
                        public void packetReceived(PlayCommand message) {
                            Clock.Instant time = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            Clock.Instant correctedTime = time.sub(NetworkHelper.offset);
                            LogHelper.i(TAG, "Corrected time ", time, " by ", NetworkHelper.offset, " to ", correctedTime);
                            PlayAction action = new PlayAction(correctedTime);
                            localAudioMixer.pushAction(action);
                        }
                    });
                    connection.addHandler(PauseCommand.class, new MessageConnection.MessageListener<PauseCommand, PauseCommand.Builder>() {
                        @Override
                        public void packetReceived(PauseCommand message) {
                            Clock.Instant time = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            Clock.Instant correctedTime = time.sub(NetworkHelper.offset);
                            PlayAction action = new PlayAction(correctedTime);
                            localAudioMixer.pushAction(action);
                        }
                    });
                    connection.addHandler(Music.class, new MessageConnection.MessageListener<Music, Music.Builder>() {
                        @Override
                        public void packetReceived(Music message) {
                            Clock.Instant playTime = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            Clock.Instant correctedPlayTime = playTime.sub(NetworkHelper.offset);
                            LogHelper.i(TAG, "Corrected time ", playTime, " by ", NetworkHelper.offset, " to ", correctedPlayTime);
                            localAudioMixer.pushFrame(new MP3MediaInfo(44100, 1, 0, MP3Encoding.UNSIGNED16), correctedPlayTime, message.data.asByteBuffer());
                        }
                    });
                }
            });
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sink.release();
        super.onDestroy();
    }
}
