package rocks.stalin.android.app.decoding;

import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 4/24/17.
 */

public class MP3MediaInfo {
    public final long sampleRate;
    public final int channels;
    public final long frameSize;
    public final MP3Encoding encoding;
    private final long frameSizeInSamples;

    public MP3MediaInfo(long sampleRate, int channels, long frameSize, MP3Encoding encoding) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.encoding = encoding;
        this.frameSize = frameSize;
        this.frameSizeInSamples = frameSize / (channels * encoding.getSampleSize());
    }

    public long sampleToFrame(long sample) {
        return sample / frameSizeInSamples;
    }

    public int getSampleSize() {
        return encoding.getSampleSize() * channels;
    }

    public Clock.Duration timeToPlayBytes(long bytes) {
        return Clock.Duration.FromNanos((bytes * 1000000000) / (getSampleSize() * sampleRate));
    }

    public int bytesPlayedInTime(Clock.Duration duration) {
        return (int) ((sampleRate * duration.inMillis() * getSampleSize()) / 1000);
    }
}
