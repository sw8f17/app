package rocks.stalin.android.app.playback;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okio.Sink;
import rocks.stalin.android.app.MP3Decoder;
import rocks.stalin.android.app.MP3MediaInfo;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.MP3File;

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

    private AudioSink sink;

    private OnPreparedListener preparedListener;
    private OnSeekCompleteListener seekCompleteListener;

    private SampleQueue queue;
    private MP3MediaInfo mediaInfo;

    public PluggableMediaPlayer() {
        decoder = new MP3Decoder();
        state = PlaybackState.Stopped;
        queue = new SampleQueue();
    }

    @Override
    public void setAudioStreamType(int streamMusic) {
    }

    @Override
    public void prepareAsync() {
        state = PlaybackState.Stopped;

        double frameSizeInSamples = mediaInfo.frameSize / (mediaInfo.encoding.getSampleSize() * mediaInfo.channels);
        double frameTime = (frameSizeInSamples / mediaInfo.sampleRate) * 1000;

        MediaPlayerFeeder feeder = new MediaPlayerFeeder(new MediaBuffer(currentFile), mediaInfo, queue);
        feederHandle = service.scheduleAtFixedRate(feeder, 0, Math.round(frameTime), TimeUnit.MILLISECONDS);
        sink.change(mediaInfo, this);
        preparedListener.onPrepared(this);
    }

    @Override
    public void reset() {
        sink.reset();
        currentFile.close();
        currentFile = null;
        state = PlaybackState.Stopped;
        feederHandle.cancel(false);
    }

    @Override
    public void release() {
        sink.reset();
        currentFile.close();
        service.shutdown();
        decoder.exit();
    }

    public void plugSink(AudioSink sink) {
        this.sink = sink;
    }

    @Override
    public void start() {
        LogHelper.e(TAG, "Starting playback");
        if(state == PlaybackState.Stopped) {
            sink.play();
        } else {
            sink.resume();
        }
        state = PlaybackState.Playing;

    }

    @Override
    public void pause() {
        LogHelper.e(TAG, "Pausing playback");
        sink.pause();
        state = PlaybackState.Paused;
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

    public byte[] read() {
        return queue.getCurrent();
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
        return (int) (currentFile.tell() / currentFile.getMediaInfo().sampleRate) * 1000;
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

    public class MediaBuffer {
        private MP3File file;

        public MediaBuffer(MP3File file) {
            this.file = file;
        }

        public byte[] read() {
            return file.decodeFrame();
        }
    }

    private static class MediaPlayerFeeder implements Runnable {
        private static final String TAG = LogHelper.makeLogTag(MediaPlayerFeeder.class);
        private final double frameTime;

        private MediaBuffer mediaBuffer;
        private MP3MediaInfo mediaInfo;
        private SampleQueue queue;

        public MediaPlayerFeeder(MediaBuffer mediaBuffer, MP3MediaInfo mediaInfo, SampleQueue queue) {
            this.mediaBuffer = mediaBuffer;
            this.mediaInfo = mediaInfo;
            this.queue = queue;

            double frameSizeInSamples = mediaInfo.frameSize / (mediaInfo.encoding.getSampleSize() * mediaInfo.channels);
            this.frameTime = (frameSizeInSamples / mediaInfo.sampleRate) * 1000;
        }

        @Override
        public void run() {
            LogHelper.i(TAG, "Running");
            byte[] read = mediaBuffer.read();
            long key = System.currentTimeMillis();
            Long last = queue.getCurrentKey();
            long next = last == -1 ? key : Math.round(last + frameTime);
            queue.putSample(next, read);
        }
    }
}
