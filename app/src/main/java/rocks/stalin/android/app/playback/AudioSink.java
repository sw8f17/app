package rocks.stalin.android.app.playback;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTimestamp;
import android.media.AudioTrack;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rocks.stalin.android.app.decoding.MP3Encoding;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 4/24/17.
 */

public class AudioSink {
    public static final String TAG = LogHelper.makeLogTag(AudioSink.class);

    AudioTrack at;

    private LocalAudioMixer mixer;

    private Thread audioThread;
    private Semaphore audioWriteLock;
    private Lock atLock = new ReentrantLock(true);

    private MP3MediaInfo mediaInfo;

    private long bufferStart = 0;

    public AudioSink(LocalAudioMixer mixer) {
        //We want to preload the track
        audioWriteLock = new Semaphore(0, true);
        this.mixer = mixer;
        mediaInfo = new MP3MediaInfo(44100, 1, 0, MP3Encoding.UNSIGNED16);

        audioThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        int written;
                        ByteBuffer buffer;
                        try {
                            audioWriteLock.acquire();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        try {
                            atLock.lockInterruptibly();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        try {

                            if (at.getPlayState() != AudioTrack.PLAYSTATE_PLAYING &&
                                    at.getPlayState() != AudioTrack.PLAYSTATE_PAUSED) {
                                continue;
                            }

                            AudioTimestamp timestamp = new AudioTimestamp();
                            Clock.Instant now;
                            long playbackPosition;
                            if (at.getTimestamp(timestamp)) {
                                now = Clock.fromNanos(timestamp.nanoTime);
                                playbackPosition = timestamp.framePosition;
                            } else {
                                now = Clock.getTime();
                                playbackPosition = at.getPlaybackHeadPosition();
                            }

                            LogHelper.i("VIZ-ROBOT", "PlayHead:", now);

                            int space = (int) ((at.getPlaybackHeadPosition() + at.getBufferSizeInFrames()) - bufferStart);

                            Clock.Duration expectedEnd = mediaInfo.timeToPlayBytes((bufferStart - playbackPosition) * mediaInfo.getSampleSize());

                            LogHelper.i(TAG, "At ", now, ", I'm expecting to run out of data in ", expectedEnd, ", or at ", now.add(expectedEnd));
                            LogHelper.i(TAG, "I presented ", timestamp.framePosition, " at ", now, " but i'm actually at ", at.getPlaybackHeadPosition());

                            buffer = AudioSink.this.mixer.readFor(mediaInfo, now.add(expectedEnd), space);

                            written = at.write(buffer, buffer.limit(), AudioTrack.WRITE_NON_BLOCKING);
                        } finally {
                            atLock.unlock();
                        }

                        //This sometimes happens when we pause after while it's running.
                        //Not a huge issue, but we should find some way fo fixing it -JJ 10/05-2017
                        if (written != buffer.limit()) {
                            LogHelper.e(TAG, "Buffer overflow!");
                        }
                        bufferStart += written / mediaInfo.getSampleSize();
                    }
                } catch (Exception e) {
                    LogHelper.e(TAG, "Exception in audiothread");
                    e.printStackTrace();
                }
            }
        };
        audioThread.setName("AudioSink Audio Thread");
        audioThread.start();
    }

    public void change(MP3MediaInfo mediaInfo) {
        if(at != null)
            throw new IllegalStateException("You can't change media params before a reset");
        this.mediaInfo = mediaInfo;

        AudioFormat.Builder format = new AudioFormat.Builder()
                .setSampleRate((int) mediaInfo.sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT);

        int channelMask;
        switch(mediaInfo.channels) {
            case 1:
                channelMask = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2:
                channelMask = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            default:
                throw new IllegalArgumentException("Channel count unsupported " + mediaInfo.channels);
        }
        format.setChannelMask(channelMask);

        atLock.lock();
        int minBuffer = AudioTrack.getMinBufferSize((int) mediaInfo.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
        minBuffer = minBuffer < 16384 ? minBuffer * (16384 / minBuffer + 1) : minBuffer;

        at = new AudioTrack.Builder()
                .setBufferSizeInBytes(minBuffer)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(format
                        .build()
                )
                .build();
        LogHelper.i(TAG, "Initialized new audiotrack with a buffer ", at.getBufferSizeInFrames() * mediaInfo.getSampleSize(), " bytes in size");

        atLock.unlock();
    }

    public void play() {
        audioWriteLock.release(1);

        atLock.lock();

        at.play();
        at.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
                LogHelper.i(TAG, "Instant for more data");
                if(audioWriteLock.availablePermits() > 0)
                    LogHelper.w(TAG, "Buffer underflow. It seems like we aren't reading data quickly enough. You might notice pauses");
                audioWriteLock.release();
            }
        });
        at.setPositionNotificationPeriod(at.getBufferSizeInFrames() / 2);

        atLock.unlock();
    }

    public void stop() {
        atLock.lock();

        audioWriteLock.drainPermits();
        if(at != null) {
            at.pause();
            at.flush();
            bufferStart = 0;
        }

        atLock.unlock();
    }

    public void reset() {
        LogHelper.i(TAG, "Resetting audio track");
        stop();

        atLock.lock();

        audioWriteLock.drainPermits();
        if(at != null) {
            at.release();
            at = null;
            bufferStart = 0;
        }

        atLock.unlock();
    }

    public void release() {
        audioThread.interrupt();
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setVolume(float gain1, float gain2) {
        at.setStereoVolume(gain1, gain2);
    }
}
