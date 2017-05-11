package rocks.stalin.android.app.playback;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTimestamp;
import android.media.AudioTrack;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
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

public class LocalSoundSink implements LocalAudioMixer.NewActionListener {
    public static final String TAG = LogHelper.makeLogTag(LocalSoundSink.class);

    private Timer timer = new Timer("SMUS - Action Scheduler", true);
    private ActionTask scheduled = null;

    AudioTrack at;

    private LocalAudioMixer mixer;

    private Thread audioThread;
    private Semaphore audioWriteLock;
    private Lock atLock = new ReentrantLock(true);

    private MP3MediaInfo mediaInfo;

    private long bufferStart = 0;

    public LocalSoundSink(LocalAudioMixer mixer) {
        //We want to preload the track
        audioWriteLock = new Semaphore(0, true);
        this.mixer = mixer;
        mixer.setNewActionListener(this);
        mediaInfo = new MP3MediaInfo(44100, 1, 0, MP3Encoding.UNSIGNED16);

        audioThread = new Thread() {
            @Override
            public void run() {
                while(true) {
                    int written;
                    ByteBuffer buffer;
                    try {
                        audioWriteLock.acquire();

                        atLock.lockInterruptibly();

                        AudioTimestamp timestamp = new AudioTimestamp();
                        Clock.Instant now;
                        if (at.getTimestamp(timestamp)) {
                            now = Clock.fromNanos(timestamp.nanoTime);
                        } else {
                            now = Clock.getTime();
                        }

                        LogHelper.i("VIZ-ROBOT", "PlayHead:", now);

                        long playbackPosition = timestamp.framePosition;
                        int space = (int) ((at.getPlaybackHeadPosition() + at.getBufferSizeInFrames()) - bufferStart);

                        Clock.Duration expectedEnd = mediaInfo.timeToPlayBytes((bufferStart - playbackPosition) * mediaInfo.getSampleSize());

                        LogHelper.i(TAG, "At ", now, ", I'm expecting to run out of data in ", expectedEnd, ", or at ", now.add(expectedEnd));
                        LogHelper.i(TAG, "I presented ", timestamp.framePosition, " at ", now, " but i'm actually at ", at.getPlaybackHeadPosition());

                        buffer = LocalSoundSink.this.mixer.readFor(mediaInfo, now.add(expectedEnd), space);

                        written = at.write(buffer, buffer.limit(), AudioTrack.WRITE_NON_BLOCKING);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LogHelper.i(TAG, "Audio feeder thread interrupted");
                        return;
                    } finally {
                        atLock.unlock();
                    }

                    if (atLock.tryLock())
                        atLock.unlock();

                    //This sometimes happens when we pause after while it's running.
                    //Not a huge issue, but we should find some way fo fixing it -JJ 10/05-2017
                    if (written != buffer.limit()) {
                        LogHelper.e(TAG, "Buffer overflow!");
                        throw new RuntimeException(String.format("Buffer overflow! Had: %d, but only wrote %d", buffer.limit(), written));
                    }
                    bufferStart += written / mediaInfo.getSampleSize();
                }
            }
        };
        audioThread.setName("LocalSoundSink Audio Thread");
        audioThread.start();
    }

    public void change(MP3MediaInfo mediaInfo) {
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

        atLock.lock();

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

    private Lock actionLock = new ReentrantLock(true);

    @Override
    public boolean onNewAction(final TimedAction action) {
        actionLock.lock();

        try {
            Clock.Instant now = Clock.getTime();
            if (action.getTime().before(now)) {
                LogHelper.w(TAG, "Woops, we missed that deadline. Let's just do it now");
                action.execute(this, mixer);
                return true;
            }
            //If the new is before the currently scheduled action we need to cancel the current
            //and schedule the new
            if (scheduled != null && action.getTime().before(scheduled.getTime())) {
                LogHelper.i(TAG, "Evicting currently scheduled task");
                mixer.pushAction(scheduled.getAction());
                timer.cancel();
                scheduled = null;
            }
            //If the scheduled action happened before now it should already have fired
            if (scheduled == null || scheduled.getTime().before(now)) {
                LogHelper.i(TAG, "Scheduling ", action, " for execution at ", action.getTime());
                scheduled = new ActionTask(action, this, mixer, this, atLock);
                timer.schedule(scheduled, new Date(action.getTime().inMillis()));
                return true;
            }
        } finally {
            actionLock.unlock();
        }
        return false;
    }

    private static class ActionTask extends TimerTask {
        private TimedAction action;
        private LocalSoundSink sink;
        private LocalAudioMixer.NewActionListener listener;
        private Lock atLock;
        private AudioMixer mixer;

        public ActionTask(TimedAction action, LocalSoundSink sink, AudioMixer mixer, LocalAudioMixer.NewActionListener listener, Lock atLock) {
            this.action = action;
            this.sink = sink;
            this.mixer = mixer;
            this.listener = listener;
            this.atLock = atLock;
        }

        public TimedAction getAction() {
            return action;
        }

        public Clock.Instant getTime() {
            return action.getTime();
        }

        @Override
        public void run() {
            LogHelper.i(TAG, "Executing action ", action);

            atLock.lock();
            action.execute(sink, mixer);
            atLock.unlock();

            TimedAction nextAction = mixer.readAction();
            if(nextAction != null)
                listener.onNewAction(nextAction);
        }
    }
}
