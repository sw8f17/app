package rocks.stalin.android.app.playback;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.sql.Time;
import java.util.Arrays;
import java.util.TreeMap;

import rocks.stalin.android.app.MP3MediaInfo;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/3/17.
 */

class TimedAudioPlayer {
    private static final String TAG = LogHelper.makeLogTag(TimedAudioPlayer.class);

    TreeMap<Long, ByteBuffer> buffer;
    private long nextSilence = 0;
    private long framePosition;
    private MP3MediaInfo mediaInfo;

    TimedAudioPlayer(MP3MediaInfo mediaInfo, long startTime) {
        buffer = new TreeMap<>();

        this.mediaInfo = mediaInfo;
        nextSilence = startTime;
    }

    public long getPlaybackPosition() {
        return framePosition;
    }

    public long getNextSilence() {
        return nextSilence;
    }

    public void pushFrame(long nextTime, ByteBuffer read) {
        buffer.put(nextTime, read);
        long frameEnd = nextTime + mediaInfo.timeToPlayBytes(read.limit());
        if(frameEnd > nextSilence)
            nextSilence = frameEnd;
    }

    public ByteBuffer readFor(long time, int samples) {
        ByteBuffer mixedBuffer = ByteBuffer.allocate(samples * mediaInfo.getSampleSize());

        int missingBytes = samples * mediaInfo.getSampleSize();
        LogHelper.i(TAG, "Reading ", missingBytes, " bytes from the timed buffer");

        Long key = buffer.lowerKey(time+1);
        if(key == null) {
            LogHelper.w(TAG, "I don't have a frame for the timestamp: ", time);
            return mixedBuffer;
        }
            //throw new IllegalArgumentException(String.format("I don't have a frame for that timestamp: %d, lowest key", time));

        int offset = mediaInfo.bytesPlayedInTime(time - key);
        if(offset % mediaInfo.getSampleSize() != 0) {
            //*
            offset += mediaInfo.getSampleSize() - (offset % mediaInfo.getSampleSize());
            /*/
            offset -= offset % mediaInfo.getSampleSize();
            //*/
        }
        LogHelper.i("So i guess we played ", offset, " bytes in ", time - key, "ms, at ", mediaInfo.sampleRate, "Hz ", mediaInfo.getSampleSize());

        while(missingBytes > 0) {
            ByteBuffer accurateBuffer = buffer.get(key);
            if(offset > accurateBuffer.limit()) {
                LogHelper.w(TAG, "The previous buffer didn't reach the start of my request");
                return mixedBuffer;
            }
            int takeFromHere = Math.min(missingBytes, accurateBuffer.limit() - offset);

            LogHelper.i(TAG, "Mixing in ", takeFromHere, " bytes at ", mixedBuffer.position(), " from ", key);

            mixedBuffer.put(accurateBuffer.array(), offset, takeFromHere);

            missingBytes -= takeFromHere;
            framePosition += takeFromHere / mediaInfo.getSampleSize();
            offset = 0;
            key = buffer.higherKey(key);
            if(key == null) {
                LogHelper.w(TAG, "Shit, i guess we don't have anything to play");
                break;
            }
        }

        mixedBuffer.flip();
        mixedBuffer.limit(mixedBuffer.capacity());
        return mixedBuffer;
    }
}
