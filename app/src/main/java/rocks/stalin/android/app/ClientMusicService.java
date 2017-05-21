package rocks.stalin.android.app;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Debug;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutionException;

import rocks.stalin.android.app.decoding.MP3Encoding;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.framework.concurrent.ServiceLocator;
import rocks.stalin.android.app.framework.concurrent.TaskScheduler;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.functional.Consumer;
import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.network.OffsetSource;
import rocks.stalin.android.app.network.OffsetSourceFactory;
import rocks.stalin.android.app.network.WifiP2PConnectionFactory;
import rocks.stalin.android.app.network.WifiP2PManagerFacade;
import rocks.stalin.android.app.playback.LocalAudioMixer;
import rocks.stalin.android.app.playback.AudioSink;
import rocks.stalin.android.app.playback.MediaPlayerBackend;
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

    private TaskExecutor executor;
    private TaskScheduler scheduler;

    private LocalAudioMixer localAudioMixer;
    private AudioSink sink;
    private MediaPlayerBackend backend;
    private OffsetSource timeService = null;

    private PowerManager.WakeLock wakeLock;

    private WifiP2PManagerFacade manager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        executor = ServiceLocator.getInstance().getService(TaskExecutor.class);
        executor = ServiceLocator.getInstance().getService(TaskScheduler.class);

        WifiP2pManager rawManager = getSystemService(WifiP2pManager.class);
        WifiP2pManager.Channel channel = rawManager.initialize(this, getMainLooper(), null);
        manager = new WifiP2PManagerFacade(rawManager, channel);

        localAudioMixer = new LocalAudioMixer();
        sink = new AudioSink(localAudioMixer);
        backend = new MediaPlayerBackend(localAudioMixer, sink, scheduler);
        //Debug.startMethodTracing("trce");

        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SMUS-Client");
    }

    MP3MediaInfo mediaInfo;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_CONNECT)) {
            wakeLock.acquire();
            LogHelper.v(TAG, "Connecting to server");

            String hostname = intent.getStringExtra(CONNECT_HOST_NAME);
            int port = intent.getIntExtra(CONNECT_PORT_NAME, -1);

            WifiP2PConnectionFactory connectionFactory = new WifiP2PConnectionFactory(this, manager, executor);
            ObservableFuture<MessageConnection> connectionFuture = connectionFactory.connect(hostname, port);
            connectionFuture.setListener(new Consumer<MessageConnection>(){

                public void call(MessageConnection connection) {
                    OffsetSourceFactory offsetSourceFactory = ServiceLocator.getInstance().getService(OffsetSourceFactory.class);

                    try {
                        timeService = offsetSourceFactory.create(connection).get();
                    } catch (InterruptedException | ExecutionException e) {
                        LogHelper.e(TAG, "OffsetSource Factory failed to create" + e);
                    }

                    timeService.start();

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
                            if(timeService != null) {
                                Clock.Instant correctedTime = time.sub(timeService.getOffset());
                                LogHelper.i(TAG, "Corrected time ", time, " by ", timeService.getOffset(), " to ", correctedTime);
                                time = correctedTime;
                            }
                            PlayAction action = new PlayAction(time);
                            backend.pushAction(action);
                        }
                    });
                    connection.addHandler(PauseCommand.class, new MessageConnection.MessageListener<PauseCommand, PauseCommand.Builder>() {
                        @Override
                        public void packetReceived(PauseCommand message) {
                            Clock.Instant time = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            if(timeService != null) {
                                time = time.sub(timeService.getOffset());
                            }
                            PauseAction action = new PauseAction(time);
                            backend.pushAction(action);
                        }
                    });
                    connection.addHandler(SongChangeCommand.class, new MessageConnection.MessageListener<SongChangeCommand, SongChangeCommand.Builder>() {
                        @Override
                        public void packetReceived(SongChangeCommand message) {
                            Clock.Instant time = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            if(timeService != null) {
                                time = time.sub(timeService.getOffset());
                            }

                            MediaInfo newInfo = message.songmetadata.mediainfo;

                            MP3Encoding encoding = MP3Encoding.UNSIGNED16;
                            mediaInfo = new MP3MediaInfo(newInfo.samplerate, newInfo.channels, newInfo.framesize, encoding);

                            MediaChangeAction action = new MediaChangeAction(time, mediaInfo);
                            backend.pushAction(action);
                        }
                    });
                    connection.addHandler(Music.class, new MessageConnection.MessageListener<Music, Music.Builder>() {
                        @Override
                        public void packetReceived(Music message) {
                            Clock.Instant playTime = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                            if(timeService != null) {
                                Clock.Instant correctedPlayTime = playTime.sub(timeService.getOffset());
                                LogHelper.i(TAG, "Corrected time ", playTime, " by ", timeService.getOffset(), " to ", correctedPlayTime);
                                playTime = correctedPlayTime;
                            }
                            backend.pushFrame(mediaInfo, playTime, message.data.asByteBuffer());
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
        //Debug.stopMethodTracing();
        super.onDestroy();
    }
}
