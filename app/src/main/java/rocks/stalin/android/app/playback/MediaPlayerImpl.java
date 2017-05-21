package rocks.stalin.android.app.playback;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import rocks.stalin.android.app.decoding.MP3Decoder;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.framework.concurrent.TaskScheduler;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFutureTask;
import rocks.stalin.android.app.framework.functional.Consumer;
import rocks.stalin.android.app.playback.actions.MediaChangeAction;
import rocks.stalin.android.app.playback.actions.PauseAction;
import rocks.stalin.android.app.playback.actions.PlayAction;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.decoding.MP3File;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 4/24/17.
 */

public class MediaPlayerImpl implements MediaPlayer {
    private static final String TAG = LogHelper.makeLogTag(MediaPlayerImpl.class);

    private TaskScheduler scheduler;

    private MP3Decoder decoder;
    private MP3File currentFile;

    private PlaybackState state;

    private TimedEventQueue backend;

    private OnPreparedListener preparedListener;
    private OnSeekCompleteListener seekCompleteListener;

    private List<TimedEventQueue> slaves;
    private MP3MediaInfo mediaInfo;

    private VirtualMediaPlayer feeder;

    PowerManager.WakeLock wakeLock;

    private int pauseSample;

    public MediaPlayerImpl(TaskScheduler scheduler) {
        state = PlaybackState.Idle;

        this.scheduler = scheduler;

        decoder = new MP3Decoder();
        decoder.init();


        LocalAudioMixer mixer = new LocalAudioMixer();
        AudioSink sink = new AudioSink(mixer);
        backend = new MediaPlayerBackend(mixer, sink, scheduler);

        slaves = new ArrayList<>();
        slaves.add(backend);
    }

    @Override
    public void setAudioStreamType(int streamMusic) {
    }

    @Override
    public void reset() {
        state = PlaybackState.Stopped;
        if(feeder != null && feeder.isRunning()) {
            feeder.stop();
        }
        feeder = null;
        if(currentFile != null) {
            currentFile.close();
            currentFile = null;
        }
        mediaInfo = null;

        state = PlaybackState.Idle;
    }

    @Override
    public void setDataSource(Context mContext, Uri uriSource) throws IOException {
        if(state != PlaybackState.Idle) {
            throw new IllegalStateException("You can't set the datasource from " + state);
        }

        currentFile = decoder.open(mContext, uriSource);
        mediaInfo = currentFile.getMediaInfo();

        Clock.Instant time = Clock.getTime();
        MediaChangeAction action = new MediaChangeAction(time, new MP3MediaInfo(mediaInfo.sampleRate, 1, mediaInfo.frameSize/mediaInfo.channels, mediaInfo.encoding));
        backend.pushAction(action);

        state = PlaybackState.Initialized;
    }

    @Override
    public void setDataSource(String source) {
        throw new UnsupportedOperationException("We can't stream urls yet, maybe some day");
    }

    @Override
    public void prepareAsync() {
        if(state != PlaybackState.Initialized) {
            throw new IllegalStateException("You can't prepareAsync from " + state);
        }

        state = PlaybackState.Preparing;

        ObservableFutureTask<Void> prepareTask = new ObservableFutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                feeder = new VirtualMediaPlayer(currentFile, mediaInfo, slaves, scheduler);
                return null;
            }
        });
        prepareTask.setListener(new Consumer<Void>() {
            @Override
            public void call(Void value) {
                state = PlaybackState.Prepared;
                preparedListener.onPrepared(MediaPlayerImpl.this);
            }
        });
        scheduler.submit(prepareTask);
    }

    @Override
    public void start() {
        if(state != PlaybackState.Prepared && state != PlaybackState.Playing && state != PlaybackState.Paused) {
            throw new IllegalStateException("You can't start from " + state);
        }

        if(wakeLock != null)
            wakeLock.acquire();

        LogHelper.e(TAG, "Starting playback");

        Clock.Instant startTime = Clock.getTime().add(Clock.Duration.fromMillis(1000));

        currentFile.seek(pauseSample);

        feeder.setStartTime(startTime);
        feeder.start();

        PlayAction action = new PlayAction(startTime);
        backend.pushAction(action);

        state = PlaybackState.Playing;
    }

    @Override
    public void pause() {
        if(state != PlaybackState.Playing) {
            throw new IllegalStateException("You can't pause from " + state);
        }

        if(wakeLock != null)
            wakeLock.release();

        LogHelper.e(TAG, "Pausing playback");
        Clock.Instant pauseTime = Clock.getTime();

        PauseAction action = new PauseAction(pauseTime);
        backend.pushAction(action);

        feeder.stop();
        pauseSample = (int)feeder.tellAt(pauseTime);

        state = PlaybackState.Paused;
    }

    @Override
    public void seekTo(int msec) {
        LogHelper.i(TAG, "Seeking to ", msec);
        int sample = (int) ((currentFile.getMediaInfo().sampleRate * msec) / 1000);
        currentFile.seek(sample);
        seekCompleteListener.onSeekComplete(this);
    }

    @Override
    public void release() {
        state = PlaybackState.End;

        reset();
        backend.release();
        decoder.exit();

        feeder.release();

        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    public boolean isPlaying() {
        return state == PlaybackState.Playing;
    }

    @Override
    public int getCurrentPosition() {
        if(state == PlaybackState.Paused) {
            return (int) mediaInfo.timeToPlayBytes(pauseSample * mediaInfo.getSampleSize()).inMillis();
        } else if(state == PlaybackState.Playing) {
            Clock.Instant now = Clock.getTime();
            return (int) mediaInfo.timeToPlayBytes(feeder.tellAt(now) * mediaInfo.getSampleSize()).inMillis();
        } else {
            return 0;
        }
    }

    @Override
    public void setVolume(float gain1, float gain2) {
    }

    @Override
    public void setWakeMode(Context context, int flags) {
        PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(flags, "SMUS-Playing");
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        this.preparedListener = listener;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
    }

    @Override
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        this.seekCompleteListener = listener;
    }

    @Override
    public void connectBackend(TimedEventQueue remoteBackend) {
        slaves.add(backend);
    }

}
