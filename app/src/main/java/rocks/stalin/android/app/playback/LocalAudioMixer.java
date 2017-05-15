package rocks.stalin.android.app.playback;

import com.google.android.gms.cast.MediaInfo;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/3/17.
 */

public class LocalAudioMixer implements AudioMixer {
    private static final String TAG = LogHelper.makeLogTag(LocalAudioMixer.class);
    private static final String MACH_TAG = "VIZ-ROBOT";

    private Lock bufferLock = new ReentrantLock(true);
    private ByteBuffer nativeBuffer;
    private Clock.Instant bufferStart;
    private PriorityQueue<TimedAction> actions;

    private NewActionListener newActionListener;

    private MP3MediaInfo bufferMediaInfo;

    public LocalAudioMixer() {
        actions = new PriorityQueue<>();
    }

    public void setNewActionListener(NewActionListener listener) {
        newActionListener = listener;
    }

    @Override
    public void change(MP3MediaInfo mediaInfo) {
        //Hardcoded buffer max size of 10
        bufferLock.lock();
        try {
            nativeBuffer = ByteBuffer.allocate((int) (mediaInfo.frameSize * 10));
            bufferStart = null;
            this.bufferMediaInfo = mediaInfo;
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public void pushFrame(MP3MediaInfo mediaInfo, Clock.Instant timeToPlay, ByteBuffer soundData) {
        LogHelper.i(MACH_TAG, "Frame:", timeToPlay, "@", mediaInfo.timeToPlayBytes(soundData.capacity()));


        bufferLock.lock();
        try {
            if(bufferStart == null)
                bufferStart = timeToPlay;

            //Offset the time of the start of the buffer to fit with the new start after a compact
            int position = nativeBuffer.position();
            Clock.Duration playTimeInto = bufferMediaInfo.timeToPlayBytes(position);

            bufferStart = bufferStart.add(playTimeInto);

            if(timeToPlay.before(bufferStart))
                throw new IllegalArgumentException("We can't mix in a frame earlier than the start of the mixer buffer");

            //Compact the buffer, copying all the remaining data to the start of the buffer
            //also sets the position to the end of the limit to make us ready for the next write
            nativeBuffer.compact();

            //Put the new sounddata into the buffer at the correct offset to fit with the time.
            Clock.Duration diff = bufferStart.timeBetween(timeToPlay);
            int offset = mediaInfo.bytesPlayedInTime(diff);
            if(offset % bufferMediaInfo.getSampleSize() != 0) {
                offset -= offset % bufferMediaInfo.getSampleSize();
            }

            if(nativeBuffer.position() != offset) {
                LogHelper.w(TAG, "Bad offset, we are wrong by ", offset - nativeBuffer.position());
                if(Math.abs(offset - nativeBuffer.position()) < 1000)
                    offset = nativeBuffer.position();
            }

            nativeBuffer.position(offset);

            int bufferSpace = nativeBuffer.limit() - nativeBuffer.position();
            if(soundData.limit() - soundData.position() > bufferSpace) {
                LogHelper.w(TAG, "Sound buffer overflowed, discarding bytes");
                soundData.limit(soundData.position() + bufferSpace);
            }
            nativeBuffer.put(soundData);

            //Flip the buffer to make us ready for the next reads
            nativeBuffer.flip();

        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public void pushAction(TimedAction action) {
        LogHelper.i(MACH_TAG, "Action:", action, "@", action.getTime());
        if(!newActionListener.onNewAction(action))
            actions.add(action);
    }

    public TimedAction readAction() {
        return actions.poll();
    }

    public ByteBuffer readFor(MP3MediaInfo mediaInfo, Clock.Instant time, int samples) {
        int missingBytes = samples * mediaInfo.getSampleSize();
        LogHelper.i(TAG, "Reading ", missingBytes, " bytes from the timed buffer");

        ByteBuffer mixedBuffer = ByteBuffer.allocate(missingBytes);

        if(bufferStart == null) {
            //If we don't have a start yet, we don't have any data
            mixedBuffer.limit(missingBytes);
            return mixedBuffer;
        }

        Clock.Duration diff = bufferStart.timeBetween(time);
        int offset = bufferMediaInfo.bytesPlayedInTime(diff);
        if(offset % bufferMediaInfo.getSampleSize() != 0) {
            offset -= offset % bufferMediaInfo.getSampleSize();
        }

        if(nativeBuffer.position() != offset) {
            LogHelper.w(TAG, "We will be skipping, ", offset - nativeBuffer.position(), " bytes to be exact");
            if(Math.abs(offset - nativeBuffer.position()) < 1000)
                offset = nativeBuffer.position();
        }

        bufferLock.lock();
        try{
            nativeBuffer.position(offset);
            LogHelper.i(TAG, "Getting ", missingBytes, " from ", nativeBuffer.limit() - nativeBuffer.position());
            if(offset + missingBytes > nativeBuffer.limit())
                throw new IllegalStateException("Buffer overrun, wanted " + (offset + missingBytes) + ", Had: " + nativeBuffer.limit());
            ByteBuffer dupBuffer = nativeBuffer.duplicate();
            dupBuffer.limit(offset + missingBytes);
            mixedBuffer.put(dupBuffer);
            nativeBuffer.position(dupBuffer.position());
        } finally {
            bufferLock.unlock();
        }

        mixedBuffer.flip();
        return mixedBuffer;
    }

    public void flush() {
        if(nativeBuffer != null)
            nativeBuffer.clear();
        bufferStart = null;
    }

    public interface NewActionListener {
        boolean onNewAction(TimedAction action);
    }
}

