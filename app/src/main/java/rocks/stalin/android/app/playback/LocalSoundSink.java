package rocks.stalin.android.app.playback;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/24/17.
 */

public class LocalSoundSink implements AudioMixer.NewActionListener {
    public static final String TAG = LogHelper.makeLogTag(LocalSoundSink.class);

    private Timer timer = new Timer("SMUS - Action Scheduler", true);
    private TimedAction scheduled = null;

    AudioTrack at;
    private MP3MediaInfo mediaInfo;

    private AudioMixer queue;

    private Thread audioThread;
    private Semaphore audioWriteLock;
    private int frameRatio;

    public LocalSoundSink(AudioMixer queue) {
        //We want to preload the track
        audioWriteLock = new Semaphore(1, true);
        this.queue = queue;
        queue.setNewActionListener(this);
    }

    public void change(final MP3MediaInfo mediaInfo) {
        if(at != null)
            throw new IllegalStateException("You can't change media params before a reset");
        this.mediaInfo = mediaInfo;

        AudioFormat.Builder format = new AudioFormat.Builder()
                .setSampleRate((int) mediaInfo.sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT);

        switch(mediaInfo.channels) {
            case 1:
                format.setChannelMask(AudioFormat.CHANNEL_OUT_MONO);
                break;
            case 2:
                format.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO);
                break;
        }

        frameRatio = mediaInfo.encoding.getSampleSize() * mediaInfo.channels;

        at = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(format
                        .build()
                )
                .build();

        at.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
                LogHelper.d(TAG, "Time for more data");
                if(audioWriteLock.availablePermits() > 0)
                    LogHelper.w(TAG, "Buffer underflow. It seems like we aren't reading data quickly enough. You might notice pauses");
                audioWriteLock.release();
            }
        });
        at.setPositionNotificationPeriod(at.getBufferSizeInFrames() / 2);

        audioThread = new Thread() {
            private long bufferStart = 0;
            @Override
            public void run() {
                while(true) {
                    try {
                        audioWriteLock.acquire();

                        long time = System.currentTimeMillis();
                        int playbackPosition = at.getPlaybackHeadPosition();
                        int space = (int) ((playbackPosition + at.getBufferSizeInFrames()) - bufferStart);

                        long expectedEnd = mediaInfo.timeToPlayBytes((bufferStart - playbackPosition) * mediaInfo.getSampleSize());

                        LogHelper.i(TAG, "At ", time, "ms, I'm expecting to run out of data in ", expectedEnd, "ms, or at ", time + expectedEnd, "ms");

                        ByteBuffer buffer = queue.readFor(time + expectedEnd, space);

                        int written = at.write(buffer, buffer.limit(), AudioTrack.WRITE_NON_BLOCKING);
                        if(written != buffer.limit())
                            throw new RuntimeException(String.format("Buffer overflow! Had: %d, but only wrote %d", buffer.limit(), written));
                        bufferStart += written / mediaInfo.getSampleSize();

                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        audioThread.setName("LocalSoundSink Audio Thread");
        audioThread.start();
    }

    public void play() {
        at.play();
    }

    public void pause() {
        at.pause();
    }

    public void stop() {
        if(at != null) {
            at.pause();
            at.flush();
            at.stop();
        }
    }

    public void resume() {
        //The audiotrack will only play audio if the buffer is full at start -JJ 27/04-2017
        //Apparently it's pretty common that the write goes though as 100%, but doesn't fill up
        //The buffer. We ask it to write twice to circumvent this issue. We can do this because
        //the audioThread implementation isn't actually required to run at the same speed as the
        //audiotrack.
        audioWriteLock.release(1);
        at.play();
    }

    public void reset() {
        audioThread.interrupt();
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        stop();
        at.release();
        //Reset the write lock to buffer track
        audioWriteLock.drainPermits();
        audioWriteLock.release(1);
        at = null;
    }

    @Override
    public boolean onNewAction(final TimedAction action) {
        long currentTime = System.currentTimeMillis();
        if(action.getTime() < currentTime) {
            LogHelper.w(TAG, "Woops, we missed that deadline. Let's just do it now");
            action.execute(at);
            return true;
        }
        //If the new is before the currently scheduled action we need to cancel the current
        //and schedule the new
        if(scheduled != null && action.getTime() < scheduled.getTime()) {
            queue.pushAction(scheduled);
            timer.cancel();
            scheduled = null;
        }
        if(scheduled == null) {
            scheduled = action;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    scheduled = null;
                    action.execute(at);
                }
            }, new Date(action.getTime()));
            return true;
        }
        return false;
    }
}
