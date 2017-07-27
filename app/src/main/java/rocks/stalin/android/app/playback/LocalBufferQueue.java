package rocks.stalin.android.app.playback;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/3/17.
 */

public class LocalBufferQueue {
    private static final String TAG = LogHelper.makeLogTag(LocalBufferQueue.class);

    private BlockingQueue<Clock.Instant> buffers;

    public LocalBufferQueue() {
        buffers = new PriorityBlockingQueue<>();
    }

    public void pushBuffer(Clock.Instant handle, Clock.Duration duration) {
        buffers.add(handle);
    }

    public void flush() {
        buffers.clear();
    }
}

