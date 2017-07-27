package rocks.stalin.android.app.playback;

import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rocks.stalin.android.app.decoding.MediaInfo;
import rocks.stalin.android.app.framework.concurrent.TaskScheduler;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

import static android.content.ContentValues.TAG;

public class MediaPlayerBackend implements ActionStrategy, TimedEventQueue {
    private static final String MACH_TAG = "VIZ-ROBOT";

    private LocalBufferQueue queue;
    private BetterAudioSink betterAudioSink;
    private TaskScheduler scheduler;

    private MediaInfo currentMediaInfo;

    private Future<?> timerHandle;
    private ActionTask scheduled = null;

    private Lock actionLock = new ReentrantLock(true);

    private PriorityQueue<TimedAction> actions;


    public MediaPlayerBackend(LocalBufferQueue queue, BetterAudioSink betterAudioSink, TaskScheduler scheduler){
        this.queue = queue;
        this.betterAudioSink = betterAudioSink;
        this.scheduler = scheduler;

        this.actions = new PriorityQueue<>();
    }

    @Override
    public void pushAction(TimedAction action) {
        LogHelper.i(MACH_TAG, "Action:", action, "@", action.getTime());
        actionLock.lock();

        try {
            Clock.Instant now = Clock.getTime();
            if (action.getTime().before(now)) {
                LogHelper.w(TAG, "Woops, we missed that deadline. Let's just do it now");
                action.execute(this);
                return ;
            }
            //If the new is before the currently scheduled action we need to cancel the current
            //and schedule the new
            if (scheduled != null && action.getTime().before(scheduled.getTime())) {
                LogHelper.i(TAG, "Evicting currently scheduled task");
                timerHandle.cancel(true);
                actions.add(scheduled.action);
                scheduled = null;
            }
            //If the scheduled action happened before now it should already have fired
            if (scheduled == null || scheduled.getTime().before(now)) {
                LogHelper.i(TAG, "Scheduling ", action, " for execution at ", action.getTime());
                scheduled = new ActionTask(action, this);
                Clock.Duration waitTime = action.getTime().sub(now);
                timerHandle = scheduler.schedule(scheduled, waitTime.inNanos(), TimeUnit.NANOSECONDS);
                return;
            }
        } finally {
            actionLock.unlock();
        }
        actions.add(action);
    }

    @Override
    public void pushFrame(MediaInfo cMI, Clock.Instant timestamp, ByteBuffer data) {
        //mixer.pushFrame(cMI, timestamp, data);
    }

    private ByteBuffer copy(ByteBuffer src) {
        // Create clone with same capacity as original.
        final ByteBuffer clone = (src.isDirect()) ?
                ByteBuffer.allocateDirect(src.capacity()) :
                ByteBuffer.allocate(src.capacity());

        clone.put(src);
        clone.flip();

        return clone;
    }

    public Clock.Duration calculateBufferTime(ByteBuffer buffer) {
        return currentMediaInfo.timeToPlayBytes(buffer.limit());
    }

    public void pushBuffer(ByteBuffer buffer, Clock.Instant presentationOffset) {
        queue.pushBuffer(presentationOffset, calculateBufferTime(buffer));
        betterAudioSink.queueAudio(copy(buffer), presentationOffset);
    }

    @Override
    public void play() {
        //sink.play();
        betterAudioSink.play();
    }

    @Override
    public void pause() {
        //sink.stop();
        betterAudioSink.pause();
    }

    @Override
    public void changeMedia(MediaInfo mediaInfo) {
        this.currentMediaInfo = mediaInfo;
        betterAudioSink.reset();
        betterAudioSink.setMediaType(mediaInfo);
    }

    private void finalizeAction() {
        TimedAction nextAction = actions.poll();
        if(nextAction != null)
            pushAction(nextAction);
    }


    @Override
    public void release() {
        //sink.release();
    }

    private static class ActionTask implements Runnable {
        private TimedAction action;
        private MediaPlayerBackend backend;

        private ActionTask(TimedAction action, MediaPlayerBackend backend) {
            this.action = action;
            this.backend = backend;
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

            action.execute(backend);
            backend.finalizeAction();
        }
    }
}
