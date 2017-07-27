package rocks.stalin.android.app.playback;

import android.media.MediaFormat;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rocks.stalin.android.app.decoding.MP3Encoding;
import rocks.stalin.android.app.decoding.MediaInfo;
import rocks.stalin.android.app.decoding.MediaReader;
import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.ScopedLock;
import rocks.stalin.android.app.framework.concurrent.TaskScheduler;
import rocks.stalin.android.app.framework.concurrent.TimeAwareRunnable;
import rocks.stalin.android.app.framework.functional.Consumer;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

class BetterVirtualMediaPlayer implements Lifecycle, TimeAwareRunnable {
    private static final String TAG = LogHelper.makeLogTag(BetterVirtualMediaPlayer.class);
    public static final int PRELOAD_SIZE = 7;
    private final TaskScheduler scheduler;

    private ScheduledFuture<?> taskHandle = null;
    private Lock actionLock = new ReentrantLock(true);
    private boolean running = false;


    private final MediaReader reader;
    private final MediaInfo format;
    private List<TimedEventQueue> slaves;
    private BlockingQueue<MediaReader.Handle> handles;

    private Keyframe lastKey;
    private Clock.Instant lastFrameMediaTime;

    public BetterVirtualMediaPlayer(MediaReader reader, MediaInfo format, List<TimedEventQueue> slaves, TaskScheduler scheduler) {
        this.scheduler = scheduler;

        this.reader = reader;
        this.format = format;
        this.slaves = slaves;
        this.handles = new PriorityBlockingQueue<>();
        lastFrameMediaTime = new Clock.Instant(0, 0);

        reader.setOnBufferAvailable(new OnBufferAvailableCB());
    }

    private class OnBufferAvailableCB implements Consumer<MediaReader.Handle> {
        public void call(MediaReader.Handle handle) {
            addBuffer(handle);
        }
    }

    private void addBuffer(MediaReader.Handle handle) {
        try(ScopedLock lock = new ScopedLock(actionLock)) {
            handles.add(handle);
        }
    }

    private void schedule() {
        LogHelper.i(TAG, "Scheduling next run");
        try {
            MediaReader.Handle next = handles.take();

            Clock.Duration playTime = next.getPresentationOffset().sub(lastKey.mediaTime);
            Clock.Instant nextTime = lastKey.realTime.add(playTime);
            try(ScopedLock lock = new ScopedLock(actionLock)) {
                handles.add(next);
                Clock.Duration thisTime = nextTime.sub(Clock.getTime());
                // @HACK: The 3 second buffering really should be handled somewhere else
                // Maybe in a strategy class or something?
                long delay = thisTime.sub(Clock.Duration.fromMillis(3000)).inMillis();
                LogHelper.i(TAG, "Scheduling BVMP in ", delay);
                taskHandle = scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            //TODO: What do do about this
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            try (ScopedLock lock = new ScopedLock(actionLock)) {
                try {
                    MediaReader.Handle next = handles.take();
                    Clock.Duration diff = next.getPresentationOffset().sub(lastKey.mediaTime);

                    Clock.Instant thisTime = lastKey.realTime.add(diff);
                    MediaInfo mediaInfo = new MediaInfo(next.getSampleRate(), next.getChannelCount(), next.info.size, MP3Encoding.UNSIGNED16);
                    LogHelper.e(TAG, "Sending data to slaves, sample rate: ", mediaInfo.sampleRate);
                    for (TimedEventQueue slave : slaves) {
                        slave.pushFrame(mediaInfo, thisTime, next.buffer.duplicate());
                        slave.pushBuffer(next.buffer.duplicate(), next.getPresentationOffset());
                    }
                    next.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            taskHandle = null;
            schedule();
        } catch (Exception e) {
            LogHelper.e(TAG, "Exception in run");
            e.printStackTrace();
        }
    }

    public void play(Clock.Instant playTime) {
        lastKey = new Keyframe(lastFrameMediaTime, playTime);
        // Hopefully someone has put some buffer into us before this
        schedule();
    }

    public void pause(Clock.Instant pauseTime) {
        taskHandle.cancel(false);
        taskHandle = null;
        lastFrameMediaTime = lastKey.mediaTime.add(pauseTime.sub(lastKey.realTime));
    }

    @Override
    public void start() {
        reader.start();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        reader.stop();
        if(taskHandle != null)
            taskHandle.cancel(true);
        taskHandle = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isLongRunning() {
        return true;
    }

    public void release() {
        reader.reset();
        if(isRunning())
            stop();
    }

    private class Keyframe {
        public Clock.Instant mediaTime;
        public Clock.Instant realTime;

        private Keyframe(Clock.Instant mediaTime, Clock.Instant realTime) {
            this.mediaTime = mediaTime;
            this.realTime = realTime;
        }
    }
}
