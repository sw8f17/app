package rocks.stalin.android.app.playback;

import android.content.Context;
import android.net.Uri;

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
import rocks.stalin.android.app.playback.actions.PauseAction;
import rocks.stalin.android.app.playback.actions.PlayAction;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.decoding.MP3File;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 4/24/17.
 */

public class PluggableMediaPlayer implements MediaPlayer {
    private static final String TAG = LogHelper.makeLogTag(PluggableMediaPlayer.class);

    private ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> feederHandle;

    private MP3Decoder decoder;
    private MP3File currentFile;

    private PlaybackState state;

    private LocalSoundSink sink;

    private OnPreparedListener preparedListener;
    private OnSeekCompleteListener seekCompleteListener;

    private LocalAudioMixer player;
    private List<AudioMixer> slaves;
    private MP3MediaInfo mediaInfo;

    MediaPlayerFeeder feeder;

    private long pauseSample;

    public PluggableMediaPlayer() {
        decoder = new MP3Decoder();
        slaves = new ArrayList<>();
        state = PlaybackState.Stopped;
    }

    @Override
    public void setAudioStreamType(int streamMusic) {
    }

    @Override
    public void prepareAsync() {
        state = PlaybackState.Stopped;

        player = new LocalAudioMixer();

        feeder = new MediaPlayerFeeder(currentFile, mediaInfo, player, slaves);

        sink = new LocalSoundSink(player);
        sink.change(mediaInfo);
        sink.initialize();
        preparedListener.onPrepared(this);
    }

    @Override
    public void reset() {
        if(sink != null) {
            sink.reset();
            currentFile.close();
            currentFile = null;
            state = PlaybackState.Stopped;
            feederHandle.cancel(false);
        }
    }

    @Override
    public void release() {
        sink.reset();
        currentFile.close();
        service.shutdown();
        decoder.exit();
    }

    @Override
    public void start() {
        LogHelper.e(TAG, "Starting playback");

        Clock.Instant startTime = Clock.getTime().add(Clock.Duration.fromMillis(100));

        currentFile.seek((int) pauseSample);

        feeder.setStartTime(startTime);
        feederHandle = service.scheduleAtFixedRate(feeder, 0, mediaInfo.timeToPlayBytes(mediaInfo.frameSize).inMillis(), TimeUnit.MILLISECONDS);

        PlayAction action = new PlayAction(startTime);
        player.pushAction(action);
        for(AudioMixer slave : slaves) {
            slave.pushAction(action);
        }

        state = PlaybackState.Playing;
    }

    @Override
    public void pause() {
        LogHelper.e(TAG, "Pausing playback");
        Clock.Instant pauseTime = Clock.getTime();
        PauseAction action = new PauseAction(pauseTime);
        player.pushAction(action);
        for(AudioMixer slave : slaves) {
            slave.pushAction(action);
        }
        state = PlaybackState.Paused;
        pauseSample = feeder.tellAt(pauseTime);
        feederHandle.cancel(true);
        player.flush();
    }

    @Override
    public void seekTo(int msec) {
        int sample = (int) ((currentFile.getMediaInfo().sampleRate * msec) / 1000);
        currentFile.seek(sample);
        seekCompleteListener.onSeekComplete(this);
    }

    @Override
    public void setDataSource(Context mContext, Uri uriSource) throws IOException {
        //TODO: Is this a good idea to do here?
        decoder.init();

        currentFile = decoder.open(mContext, uriSource);
        mediaInfo = currentFile.getMediaInfo();

        state = PlaybackState.Stopped;
    }

    @Override
    public void setDataSource(String source) {
        throw new UnsupportedOperationException("We can't stream urls yet, maybe some day");
    }

    @Override
    public boolean isPlaying() {
        return state == PlaybackState.Playing;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
        //return (int) (currentFile.tell() / currentFile.getMediaInfo().sampleRate) * 1000;
    }

    @Override
    public void setVolume(float volumeDuck, float volumeDuck1) {
    }

    @Override
    public void setWakeMode(Context applicationContext, int partialWakeLock) {
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
        public static final int PRELOAD_SIZE = 5;

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

            while(nextFrameStart.before(bufferEnd)) {
                LogHelper.i(TAG, "Inserting frame into the buffer for playback at ", nextFrameStart);
                ByteBuffer read = file.decodeFrame();
                player.pushFrame(mediaInfo, nextFrameStart, read);
                for(AudioMixer slave : slaves)
                    slave.pushFrame(mediaInfo, nextFrameStart, read);
                nextSample = file.tell();
                nextFrameStart = nextFrameStart.add(mediaInfo.timeToPlayBytes(read.limit()));
            }
        }
    }
}
