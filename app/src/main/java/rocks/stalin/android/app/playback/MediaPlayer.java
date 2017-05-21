package rocks.stalin.android.app.playback;

import android.content.Context;
import android.net.Uri;

import java.io.IOError;
import java.io.IOException;

import javax.xml.transform.ErrorListener;

/**
 * Created by delusional on 4/24/17.
 */

public interface MediaPlayer {
    void setAudioStreamType(int streamMusic);
    void prepareAsync();
    void reset();
    void release();

    void start();
    void pause();
    void seekTo(int position);

    void setDataSource(Context mContext, Uri uriSource) throws IOException;
    void setDataSource(String source) throws IOException;

    boolean isPlaying();
    int getCurrentPosition();

    void setVolume(float volumeDuck, float volumeDuck1);
    void setWakeMode(Context applicationContext, int partialWakeLock);


    void setOnPreparedListener(OnPreparedListener localPlayback);
    void setOnCompletionListener(OnCompletionListener localPlayback);
    void setOnErrorListener(OnErrorListener localPlayback);
    void setOnSeekCompleteListener(OnSeekCompleteListener localPlayback);

    void connectBackend(TimedEventQueue remoteBackend);

    interface OnCompletionListener {
        void onCompletion(MediaPlayer player);
    }

    interface OnErrorListener {
        boolean onError(MediaPlayer mp, int what, int extra);
    }

    interface OnPreparedListener {
        void onPrepared(MediaPlayer player);
    }

    interface OnSeekCompleteListener {
        void onSeekComplete(MediaPlayer mp);
    }
}
