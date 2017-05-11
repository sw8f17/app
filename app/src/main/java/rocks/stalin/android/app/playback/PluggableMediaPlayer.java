package rocks.stalin.android.app.playback;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rocks.stalin.android.app.decoding.MP3Decoder;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.actions.MediaChangeAction;
import rocks.stalin.android.app.playback.actions.PauseAction;
import rocks.stalin.android.app.playback.actions.PlayAction;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.proto.MediaInfo;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.decoding.MP3File;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 4/24/17.
 */

public class PluggableMediaPlayer implements MediaPlayer {
    private static final String TAG = LogHelper.makeLogTag(PluggableMediaPlayer.class);

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> feederHandle;

    private MP3Decoder decoder;
    private MP3File currentFile;

    private PlaybackState state;

    private LocalSoundSink sink;

    private OnPreparedListener preparedListener;
    private OnSeekCompleteListener seekCompleteListener;

    private LocalAudioMixer localMixer;
    private List<AudioMixer> slaves;
    private MP3MediaInfo mediaInfo;

    MediaPlayerFeeder feeder;

    PowerManager.WakeLock wakeLock;

    private long pauseSample;

    public PluggableMediaPlayer() {
        state = PlaybackState.Idle;

        decoder = new MP3Decoder();
        decoder.init();

        slaves = new ArrayList<>();

        localMixer = new LocalAudioMixer();
        sink = new LocalSoundSink(localMixer);

    }

    @Override
    public void setAudioStreamType(int streamMusic) {
    }

    @Override
    public void reset() {
        sink.reset();
        state = PlaybackState.Stopped;
        if(feederHandle != null) {
            feederHandle.cancel(true);
            feederHandle = null;
        }
        if(currentFile != null) {
            currentFile.close();
            currentFile = null;
        }
        localMixer.flush();

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
        pushAction(action);

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

        feeder = new MediaPlayerFeeder(currentFile, mediaInfo, localMixer, slaves);

        state = PlaybackState.Prepared;
        preparedListener.onPrepared(this);
    }

    @Override
    public void start() {
        if(state != PlaybackState.Prepared && state != PlaybackState.Playing && state != PlaybackState.Paused) {
            throw new IllegalStateException("You can't start from " + state);
        }

        if(wakeLock != null)
            wakeLock.acquire();

        LogHelper.e(TAG, "Starting playback");

        Clock.Instant startTime = Clock.getTime().add(Clock.Duration.fromMillis(100));

        currentFile.seek((int) pauseSample);

        feeder.setStartTime(startTime);
        //This may drift over time, but since we are mostly playing short tracks
        //it might be ok? -JJ 10/05-2017
        long period = mediaInfo.timeToPlayBytes(mediaInfo.frameSize).inMillis();
        feederHandle = scheduler.scheduleAtFixedRate(feeder, 0, period, TimeUnit.MILLISECONDS);

        PlayAction action = new PlayAction(startTime);
        pushAction(action);

        state = PlaybackState.Playing;
    }

    private void pushAction(TimedAction action) {
        localMixer.pushAction(action);
        for(AudioMixer slave : slaves) {
            slave.pushAction(action);
        }
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

        pushAction(action);

        feederHandle.cancel(true);
        pauseSample = feeder.tellAt(pauseTime);

        state = PlaybackState.Paused;
    }

    @Override
    public void seekTo(int msec) {
        int sample = (int) ((currentFile.getMediaInfo().sampleRate * msec) / 1000);
        currentFile.seek(sample);
        seekCompleteListener.onSeekComplete(this);
    }

    @Override
    public void release() {
        state = PlaybackState.End;

        reset();
        sink.release();
        localMixer.flush();
        decoder.exit();
        scheduler.shutdown();

        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    public boolean isPlaying() {
        return state == PlaybackState.Playing;
    }

    @Override
    public int getCurrentPosition() {
        if(state == PlaybackState.Paused)
            return (int) mediaInfo.timeToPlayBytes(pauseSample * mediaInfo.getSampleSize()).inMillis();
        Clock.Instant now = Clock.getTime();
        return (int) mediaInfo.timeToPlayBytes(feeder.tellAt(now) * mediaInfo.getSampleSize()).inMillis();
        //return (int) (currentFile.tell() / currentFile.getMediaInfo().sampleRate) * 1000;
    }

    @Override
    public void setVolume(float gain1, float gain2) {
        sink.setVolume(gain1, gain2);
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

    public void addMixer(RemoteMixer remoteMixer) {
        slaves.add(remoteMixer);
    }

    private static class MediaPlayerFeeder implements Runnable {
        private static final String TAG = LogHelper.makeLogTag(MediaPlayerFeeder.class);
        public static final int PRELOAD_SIZE = 10;

        private MP3File file;
        private MP3MediaInfo mediaInfo;
        private AudioMixer player;
        private List<AudioMixer> slaves;
        private long nextSample;
        private Clock.Instant nextFrameStart;

        public MediaPlayerFeeder(MP3File file, MP3MediaInfo mediaInfo, AudioMixer player, List<AudioMixer> slaves) {
            this.file = file;
            this.mediaInfo = mediaInfo;
            this.player = player;
            this.slaves = slaves;
        }

        public void setStartTime(Clock.Instant instant) {
            nextFrameStart = instant;
        }

        public long tellAt(Clock.Instant time) {
            if(nextFrameStart == null)
                return 0;
            Clock.Duration diff = time.timeBetween(nextFrameStart);
            long expectedSamples = mediaInfo.bytesPlayedInTime(diff) / mediaInfo.getSampleSize();

            if (time.before(nextFrameStart))
                return nextSample - expectedSamples;

            return nextSample + expectedSamples;
        }

        @Override
        public void run() {
            LogHelper.i(TAG, "Feeding the audio player");

            Clock.Instant now = Clock.getTime();

            Clock.Duration frameTime = mediaInfo.timeToPlayBytes(mediaInfo.frameSize);
            Clock.Duration preloadBufferSize = frameTime.multiply(PRELOAD_SIZE);
            Clock.Instant bufferEnd = now.add(preloadBufferSize);

            while (nextFrameStart.before(bufferEnd)) {
                LogHelper.i(TAG, "Inserting frame into the buffer for playback at ", nextFrameStart);
                ByteBuffer read = file.decodeFrame();
                ByteBuffer left = ByteBuffer.allocate(read.limit()/mediaInfo.channels);
                ByteBuffer right = ByteBuffer.allocate(read.limit()/mediaInfo.channels);

                MP3MediaInfo cMI = new MP3MediaInfo(mediaInfo.sampleRate, 1, mediaInfo.frameSize / mediaInfo.channels, mediaInfo.encoding);

                for(int i = read.position(); i < read.limit(); i += mediaInfo.getSampleSize()) {
                    for(int j = 0; j < mediaInfo.encoding.getSampleSize(); j++)
                        left.put(read.get());
                    for(int j = 0; j < mediaInfo.encoding.getSampleSize(); j++)
                        right.put(read.get());
                }
                left.flip();
                right.flip();
                player.pushFrame(cMI, nextFrameStart, left);
                for (AudioMixer slave : slaves)
                    slave.pushFrame(cMI, nextFrameStart, right);
                nextSample = file.tell();
                nextFrameStart = nextFrameStart.add(mediaInfo.timeToPlayBytes(read.limit()));
            }
        }
    }
}
