package rocks.stalin.android.app.playback;

import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.TreeMap;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/3/17.
 */

class AudioMixer {
    private static final String TAG = LogHelper.makeLogTag(AudioMixer.class);

    private TreeMap<Long, ByteBuffer> buffer;
    private PriorityQueue<TimedAction> actions;
    private long nextSilence = 0;
    private long framePosition;
    private MP3MediaInfo mediaInfo;

    private NewActionListener newActionListener;

    AudioMixer(MP3MediaInfo mediaInfo, long startTime) {
        buffer = new TreeMap<>();
        actions = new PriorityQueue<>();

        this.mediaInfo = mediaInfo;
        nextSilence = startTime;
    }

    public void setNewActionListener(NewActionListener listener) {
        newActionListener = listener;
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

    public void pushAction(TimedAction action) {
        if(!newActionListener.onNewAction(action))
            actions.add(action);
    }

    public TimedAction readAction() {
        return actions.poll();
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

    public interface NewActionListener {
        boolean onNewAction(TimedAction action);
    }
}

