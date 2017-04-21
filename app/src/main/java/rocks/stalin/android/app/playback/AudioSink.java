package rocks.stalin.android.app.playback;

/**
 * Created by delusional on 4/24/17.
 */

interface AudioSink {
    void play(byte[] frame);
}
