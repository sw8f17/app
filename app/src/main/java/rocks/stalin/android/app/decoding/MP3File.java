package rocks.stalin.android.app.decoding;

import java.nio.ByteBuffer;

/**
 * Created by delusional on 4/24/17.
 */

public class MP3File {
    public long context; /* Set by native */
    private MP3MediaInfo mediaInfo;

    public MP3File(long handle, long buffer, long bufferSize, int fd, MP3MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
        nativeCons(handle, buffer, bufferSize, fd);
    }
    private native void nativeCons(long handle, long buffer, long bufferSize, int fd);

    public static native void staticInit();

    public native byte[] decodeFrameNative();
    public native void seek(int sample);
    public native long tell();
    public native long tellframe();
    public native void close();

    static {
        System.loadLibrary("native-lib");
        staticInit();
    }

    public ByteBuffer decodeFrame() {
        return ByteBuffer.wrap(decodeFrameNative());
    }

    public MP3MediaInfo getMediaInfo() {
        return mediaInfo;
    }

}
