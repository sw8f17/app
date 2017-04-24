package rocks.stalin.android.app.playback;

import rocks.stalin.android.app.MP3MediaInfo;

/**
 * Created by delusional on 4/24/17.
 */

interface AudioSink {
    void change(MP3MediaInfo mediaInfo, PluggableMediaPlayer.MediaBuffer mediaBuffer);
    void play();
    void pause();
    void stop();
    void resume();

    void reset();
}
