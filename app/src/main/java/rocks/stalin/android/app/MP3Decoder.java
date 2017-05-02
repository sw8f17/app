package rocks.stalin.android.app;

/**
 * Created by delusional on 4/10/17.
 */

public class MP3Decoder {
    public native byte[] decode(String URI);

    static {
        System.loadLibrary("native-lib");
    }
}
