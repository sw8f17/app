package rocks.stalin.android.app.playback;

import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/24/17.
 */

class DebugSink implements AudioSink {
    private static final String TAG = LogHelper.makeLogTag(DebugSink.class);

    @Override
    public void play(byte[] frame) {
        LogHelper.e(TAG, "I'm supposed to play: ", frame.length, " bytes of data");
    }
}
