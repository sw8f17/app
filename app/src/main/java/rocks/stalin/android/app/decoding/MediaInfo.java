package rocks.stalin.android.app.decoding;

import android.media.AudioFormat;
import android.media.MediaFormat;
import android.os.Build;

import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 4/24/17.
 */

public class MediaInfo {
    public final long sampleRate;
    public final int channels;
    public final long frameSize;
    public final MP3Encoding encoding;
    private final long frameSizeInSamples;

    public MediaInfo(long sampleRate, int channels, long frameSize, MP3Encoding encoding) {
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
        return (int) ((sampleRate * duration.inNanos() * getSampleSize()) / 1000000000);
    }

    private static MP3Encoding getEncoding(MediaFormat format) {
        // Translate the encoding from the MediaFormat to something we know
        // If we are on an old version of android, or the key is missing we default
        // To Unsigned16
        MP3Encoding encoding = MP3Encoding.UNSIGNED16;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                switch (format.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
                    case AudioFormat.ENCODING_PCM_8BIT:
                        encoding = MP3Encoding.UNSIGNED8;
                        break;
                    case AudioFormat.ENCODING_PCM_16BIT:
                        encoding = MP3Encoding.UNSIGNED16;
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported pcm encoding");
                }
            }
        }

        return encoding;
    }

    public static MediaInfo fromFormat(MediaFormat format) {
        MP3Encoding encoding = getEncoding(format);
        return new MediaInfo(
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                0,// TODO: Remove from the struct
                encoding
        );
    }
}
