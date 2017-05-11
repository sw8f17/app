package rocks.stalin.android.app;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Debug;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import rocks.stalin.android.app.decoding.MP3Encoding;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.network.PeriodicPollOffsetProvider;
import rocks.stalin.android.app.network.SntpOffsetSource;
import rocks.stalin.android.app.network.WifiP2PMessageClient;
import rocks.stalin.android.app.playback.LocalAudioMixer;
import rocks.stalin.android.app.playback.LocalSoundSink;
import rocks.stalin.android.app.playback.actions.MediaChangeAction;
import rocks.stalin.android.app.playback.actions.PauseAction;
import rocks.stalin.android.app.playback.actions.PlayAction;
import rocks.stalin.android.app.proto.MediaInfo;
import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.PauseCommand;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.proto.SongChangeCommand;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;
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
    private PeriodicPollOffsetProvider timeService;

    private PowerManager.WakeLock wakeLock;


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
        Debug.startMethodTracing("trce");

        timeService = new PeriodicPollOffsetProvider(new SntpOffsetSource());
        timeService.start();

        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SMUS-Client");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_CONNECT)) {
            wakeLock.acquire();
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
                            Clock.Instant correctedTime = time.sub(timeService.getOffset());
                            LogHelper.i(TAG, "Corrected time ", time, " by ", timeService.getOffset(), " to ", correctedTime);
                            PlayAction action = new PlayAction(correctedTime);
                            localAudioMixer.pushAction(action);
                        }
                    });
                    connection.addHandler(PauseCommand.class, new MessageConnection.MessageListener<PauseCommand, PauseCommand.Builder>() {
                        @Override
                        public void packetReceived(PauseCommand message) {
                            Clock.Instant time = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            Clock.Instant correctedTime = time.sub(timeService.getOffset());
                            PauseAction action = new PauseAction(correctedTime);
                            localAudioMixer.pushAction(action);
                        }
                    });
                    connection.addHandler(SongChangeCommand.class, new MessageConnection.MessageListener<SongChangeCommand, SongChangeCommand.Builder>() {
                        @Override
                        public void packetReceived(SongChangeCommand message) {
                            Clock.Instant time = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            Clock.Instant correctedTime = time.sub(timeService.getOffset());

                            MediaInfo newInfo = message.songmetadata.mediainfo;

                            MP3Encoding encoding = MP3Encoding.UNSIGNED16;
                            MP3MediaInfo mediaInfo = new MP3MediaInfo(newInfo.samplerate, newInfo.channels, newInfo.framesize, encoding);

                            MediaChangeAction action = new MediaChangeAction(correctedTime, mediaInfo);
                            localAudioMixer.pushAction(action);
                        }
                    });
                    connection.addHandler(Music.class, new MessageConnection.MessageListener<Music, Music.Builder>() {
                        @Override
                        public void packetReceived(Music message) {
                            Clock.Instant playTime = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            Clock.Instant correctedPlayTime = playTime.sub(timeService.getOffset());
                            LogHelper.i(TAG, "Corrected time ", playTime, " by ", timeService.getOffset(), " to ", correctedPlayTime);
                            localAudioMixer.pushFrame(new MP3MediaInfo(44100, 1, 0, MP3Encoding.UNSIGNED16), correctedPlayTime, message.data.asByteBuffer());
                        }
                    });
                }
            });
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        sink.release();
        if(wakeLock.isHeld())
            wakeLock.release();
        timeService.release();
        Debug.stopMethodTracing();
        super.onDestroy();
    }
}
