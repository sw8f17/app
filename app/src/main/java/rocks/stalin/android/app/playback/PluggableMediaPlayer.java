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

import okio.Sink;
import rocks.stalin.android.app.MP3Decoder;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.MP3File;

/**
 * Created by delusional on 4/24/17.
 */

public class PluggableMediaPlayer implements MediaPlayer {
    private static final String TAG = LogHelper.makeLogTag(PluggableMediaPlayer.class);

    private MP3Decoder decoder;
    private MP3File currentFile;

    private PlaybackState state;

    private AudioSink sink;

    private OnPreparedListener preparedListener;

    public PluggableMediaPlayer() {
        decoder = new MP3Decoder();
        state = PlaybackState.Stopped;
        //TODO: Is this a good idea to do here?
        decoder.init();
    }

    @Override
    public void setAudioStreamType(int streamMusic) {
    }

    //This isn't called before setDataSource so the library isn't initialized.
    //We need to do something clever instead of what the fuck i'm doing now
    @Override
    public void prepareAsync() {
        state = PlaybackState.Stopped;
        preparedListener.onPrepared(this);
    }

    @Override
    public void reset() {
        sink.reset();
        currentFile.close();
        currentFile = null;
        state = PlaybackState.Stopped;
    }

    @Override
    public void release() {
        sink.reset();
        currentFile.close();
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
    public void seekTo(int position) {
    }

    @Override
    public void setDataSource(Context mContext, Uri uriSource) throws IOException {
        currentFile = decoder.open(mContext, uriSource);

        sink.change(currentFile.getMediaInfo(), new MediaBuffer(currentFile));
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
}
