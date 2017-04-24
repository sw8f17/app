package rocks.stalin.android.app;

/**
 * Created by delusional on 4/24/17.
 */

public class MP3MediaInfo {
    public final long sampleRate;
    public final int channels;
    public final long frameSize;
    public final double frameTime;
    public final double bps;
    public final MP3Encoding encoding;

    public MP3MediaInfo(long sampleRate, int channels, long frameSize, MP3Encoding encoding) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.encoding = encoding;
        this.frameSize = frameSize;

        this.frameTime = ((double)frameSize / encoding.getSampleSize()) / sampleRate;
        this.bps = (double)sampleRate * encoding.getSampleSize();
    }
}
