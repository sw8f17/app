package rocks.stalin.android.app.playback;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rocks.stalin.android.app.decoding.MediaInfo;
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
    private int lastReadEnd;
    private int lastWriteEnd;
    private Clock.Instant bufferStart;

    private MediaInfo bufferMediaInfo;

    public LocalAudioMixer() {
    }

    @Override
    public void change(MediaInfo mediaInfo) {
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

    /**
     * Redate the buffer after writing the first frame.
     *
     * <p>
     *     This is useful after preloading data into the mixer, and then setting the start time.
     *     Especially if a clock sync happens after the preload but before the play.
     * </p>
     * @param time The new time to set at the beginning of playback
     */
    @Override
    public void setStartTime(Clock.Instant time) {
        LogHelper.i(TAG, "Backdating the buffer to ", time);
        if(bufferStart != null)
            bufferStart = time;
    }

    @Override
    public void pushFrame(MediaInfo mediaInfo, Clock.Instant timeToPlay, ByteBuffer soundData) {
        LogHelper.i(MACH_TAG, "Frame:", timeToPlay, "@", mediaInfo.timeToPlayBytes(soundData.capacity()));


        bufferLock.lock();
        try {
            if(bufferStart == null)
                bufferStart = timeToPlay;

            //Offset the time of the start of the buffer to fit with the new start after a compact
            int position = nativeBuffer.position();
            Clock.Duration playTimeInto = bufferMediaInfo.timeToPlayBytes(position);

            bufferStart = bufferStart.add(playTimeInto);

            if(timeToPlay.before(bufferStart)) {
                LogHelper.w(TAG, "You can't write before the start of the buffer, You probably had a time resync");
            }

            //Compact the buffer, copying all the remaining data to the start of the buffer
            //also sets the position to the end of the limit to make us ready for the next write
            lastReadEnd -= nativeBuffer.position();
            lastWriteEnd -= nativeBuffer.position();
            nativeBuffer.compact();

            //Put the new sounddata into the buffer at the correct offset to fit with the time.
            Clock.Duration diff = timeToPlay.sub(bufferStart);
            int offset = mediaInfo.bytesPlayedInTime(diff);
            if(offset % bufferMediaInfo.getSampleSize() != 0) {
                offset -= offset % bufferMediaInfo.getSampleSize();
            }

            if(lastWriteEnd != offset) {
                if(Math.abs(offset - lastWriteEnd) < 1)
                    offset = lastWriteEnd;
                else
                    LogHelper.w(TAG, "Bad offset, we are wrong by ", offset - nativeBuffer.position());
            }

            nativeBuffer.position(offset);

            int bufferSpace = nativeBuffer.limit() - nativeBuffer.position();
            if(soundData.limit() - soundData.position() > bufferSpace) {
                LogHelper.w(TAG, "Sound buffer overflowed, discarding bytes");
                soundData.limit(soundData.position() + bufferSpace);
            }
            nativeBuffer.put(soundData);
            lastWriteEnd = nativeBuffer.position();

            //Flip the buffer to make us ready for the next reads
            nativeBuffer.flip();

        } finally {
            bufferLock.unlock();
        }
    }

    public ByteBuffer readFor(MediaInfo mediaInfo, Clock.Instant time, int samples) {
        int missingBytes = samples * mediaInfo.getSampleSize();
        //LogHelper.i(TAG, "Reading ", missingBytes, " bytes from the timed buffer");

        ByteBuffer mixedBuffer = ByteBuffer.allocate(missingBytes);

        if(bufferStart == null) {
            //If we don't have a start yet, we don't have any data
            mixedBuffer.limit(missingBytes);
            return mixedBuffer;
        }

        if(time.before(bufferStart))
            throw new IllegalStateException("You cant read before buffer starting point: " + time + " / " + bufferStart);

        Clock.Duration diff = time.sub(bufferStart);
        int offset = bufferMediaInfo.bytesPlayedInTime(diff);
        //LogHelper.i(TAG, "diff is ", diff);
        if(offset % bufferMediaInfo.getSampleSize() != 0) {
            offset -= offset % bufferMediaInfo.getSampleSize();
        }

        if(lastReadEnd != offset) {
            if(Math.abs(offset - lastReadEnd) < 1000)
                offset = lastReadEnd;
            else
                //LogHelper.w(TAG, "We will be skipping ", offset - lastReadEnd, " bytes");
                ;
        }

        bufferLock.lock();
        try{
            if(offset < 0) {
                int skipBytes = -offset;
                LogHelper.w(TAG, "underflowing ", skipBytes, " bytes off the start");
                mixedBuffer.position(skipBytes);
                missingBytes -= skipBytes;
                offset = 0;
            }
            if(offset <= nativeBuffer.limit()) {
                nativeBuffer.position(offset);
                //LogHelper.i(TAG, "Getting ", missingBytes, " from ", nativeBuffer.limit() - nativeBuffer.position());
                ByteBuffer dupBuffer = nativeBuffer.duplicate();
                dupBuffer.limit(offset + missingBytes);
                mixedBuffer.put(dupBuffer);
                if(offset + missingBytes > nativeBuffer.limit()) {
                    LogHelper.w(TAG, "underflowing ", (offset + missingBytes) - nativeBuffer.limit(), " bytes off the end");
                    mixedBuffer.position(mixedBuffer.capacity());
                }
                lastReadEnd = dupBuffer.position();
            } else {
                LogHelper.w(TAG, "The requested music is after at ", offset, " we only have data to ", nativeBuffer.limit());
                mixedBuffer.position(mixedBuffer.capacity());
                nativeBuffer.position(nativeBuffer.limit());
                lastReadEnd += missingBytes;
            }
        } finally {
            bufferLock.unlock();
        }

        mixedBuffer.flip();
        return mixedBuffer;
    }

    public void flush() {
        bufferLock.lock();
        try {
            if (nativeBuffer != null)
                nativeBuffer.clear();
            bufferStart = null;
        } finally {
            bufferLock.unlock();
        }
    }
}

