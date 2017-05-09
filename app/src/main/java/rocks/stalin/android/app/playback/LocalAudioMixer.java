package rocks.stalin.android.app.playback;

import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.TreeMap;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/3/17.
 */

public class LocalAudioMixer implements AudioMixer {
    private static final String TAG = LogHelper.makeLogTag(LocalAudioMixer.class);

    private TreeMap<Clock.Instant, ByteBuffer> buffer;
    private PriorityQueue<TimedAction> actions;
    private Clock.Instant nextSilence;
    private long framePosition;
    private MP3MediaInfo mediaInfo;

    private NewActionListener newActionListener;

    public LocalAudioMixer(MP3MediaInfo mediaInfo, Clock.Instant startTime) {
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

    public Clock.Instant getNextSilence() {
        return nextSilence;
    }

    @Override
    public void pushFrame(Clock.Instant nextTime, ByteBuffer read) {
        nextSilence = nextSilence.add(mediaInfo.timeToPlayBytes(read.capacity()));
        buffer.put(nextTime, read);
    }

    @Override
    public void pushAction(TimedAction action) {
        LogHelper.i(TAG, "Scheduling ", action, " @ ", action.getTime());
        if(!newActionListener.onNewAction(action))
            actions.add(action);
    }

    public TimedAction readAction() {
        return actions.poll();
    }

    public ByteBuffer readFor(Clock.Instant time, int samples) {
        ByteBuffer mixedBuffer = ByteBuffer.allocate(samples * mediaInfo.getSampleSize());

        int missingBytes = samples * mediaInfo.getSampleSize();
        LogHelper.i(TAG, "Reading ", missingBytes, " bytes from the timed buffer");

        Clock.Instant key = time;
        if(buffer.get(key) == null) {
            key = buffer.lowerKey(key);
            if(key == null) {
                LogHelper.w(TAG, "I don't have a frame for the timestamp: ", time, " My earliest is at ", buffer.higherKey(time));
                mixedBuffer.flip().limit(mixedBuffer.capacity());
                return mixedBuffer;
            }
        }

        //If any buffer starts in the middle, then it's always the case that its the first one
        //this is only true because there's no overlap between the frames though, if we do have some
        //overlap at some point we will have to do some actual mixing - JJ 05/05-2017
        int offset = mediaInfo.bytesPlayedInTime(time.timeBetween(key));
        if(offset % mediaInfo.getSampleSize() != 0) {
            //*
            offset += mediaInfo.getSampleSize() - (offset % mediaInfo.getSampleSize());
            /*/
            offset -= offset % mediaInfo.getSampleSize();
            //*/
        }
        LogHelper.i(TAG, "So i guess we played ", offset, " bytes in ", time.timeBetween(key), ", at ", mediaInfo.sampleRate, "Hz * ", mediaInfo.getSampleSize());

        while(missingBytes > 0) {
            ByteBuffer accurateBuffer = buffer.get(key);
            if(offset > accurateBuffer.limit()) {
                //It's possible that the last buffer didn't reach, yet another buffer might
                //intersect later. For now it's pretty unlikely, so i'll just let it skip
                // - JJ 05/05-2017
                LogHelper.w(TAG, "The previous buffer didn't reach the start of my request, I did have something at ", key);
                mixedBuffer.flip().limit(mixedBuffer.capacity());
                return mixedBuffer;
            }
            int takeFromHere = Math.min(missingBytes, accurateBuffer.limit() - offset);

            LogHelper.i(TAG, "Mixing in ", takeFromHere, " bytes at ", mixedBuffer.position(), " from ", key);

            ByteBuffer mine = accurateBuffer.duplicate();
            mine.position(offset);
            mine.limit(offset + takeFromHere);
            mixedBuffer.put(mine);

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

