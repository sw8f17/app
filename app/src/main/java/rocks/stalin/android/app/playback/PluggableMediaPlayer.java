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

import rocks.stalin.android.app.MP3Decoder;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/24/17.
 */

public class PluggableMediaPlayer implements MediaPlayer {
    private static final String TAG = LogHelper.makeLogTag(PluggableMediaPlayer.class);

    private MP3Decoder decoder;
    private long fHandle;

    private PlaybackState state;

    private AudioSink sink;

    private OnPreparedListener preparedListener;

    public PluggableMediaPlayer() {
        decoder = new MP3Decoder();
        fHandle = 0;
        state = PlaybackState.Stopped;
    }

    @Override
    public void setAudioStreamType(int streamMusic) {
    }

    //This isn't called before setDataSource so the library isn't initialized.
    //We need to do something clever instead of what the fuck i'm doing now
    @Override
    public void prepareAsync() {
        //TODO: Maybe make this async?
        //decoder.init();
        state = PlaybackState.Stopped;
        preparedListener.onPrepared(this);
    }

    @Override
    public void reset() {
        decoder.close(fHandle);
        fHandle = 0;
        state = PlaybackState.Stopped;
    }

    @Override
    public void release() {
        if(fHandle != 0)
            decoder.close(fHandle);
        fHandle = 0;
        decoder.exit();
    }

    public void plugSink(AudioSink sink) {
        this.sink = sink;
    }

    @Override
    public void start() {
        LogHelper.e(TAG, "Starting playback");
        state = PlaybackState.Playing;
        Thread worker = new Thread() {
            @Override
            public void run() {
                while(state == PlaybackState.Playing) {
                    byte[] frame = decoder.decodeFrame(fHandle);
                    if(frame.length == 0)
                        pause();
                    sink.play(frame);
                }
            }
        };
        worker.start();
    }

    @Override
    public void pause() {
        state = PlaybackState.Paused;
    }

    @Override
    public void seekTo(int position) {
    }

    @Override
    public void setDataSource(Context mContext, Uri uriSource) throws IOException {
        fHandle = decoder.setDataSource(mContext, uriSource);
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
}
