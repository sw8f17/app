package rocks.stalin.android.app.playback;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/24/17.
 */

class DebugSink implements AudioSink {
    private static final String TAG = LogHelper.makeLogTag(DebugSink.class);

    @Override
    public void play() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void change(MP3MediaInfo mediaInfo, PluggableMediaPlayer mediaBuffer) {

    }
}
