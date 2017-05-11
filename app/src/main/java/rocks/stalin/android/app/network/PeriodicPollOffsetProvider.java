package rocks.stalin.android.app.network;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/12/17.
 */

public class PeriodicPollOffsetProvider {
    private static final String TAG = LogHelper.makeLogTag(PeriodicPollOffsetProvider.class);

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ScheduledFuture<?> taskHandle;

    private long currentOffset;
    private SntpOffsetSource offsetSource;

    public PeriodicPollOffsetProvider(SntpOffsetSource offsetSource) {
        this.offsetSource = offsetSource;
        currentOffset = 0;
    }

    public void start() {
        taskHandle = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    currentOffset = offsetSource.getOffset();
                    LogHelper.i(TAG, "New sntp offset: ", currentOffset);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        taskHandle.cancel(false);
    }

    public void release() {
        taskHandle.cancel(true);
        scheduler.shutdown();
    }

    public Clock.Duration getOffset() {
        return new Clock.Duration(currentOffset, 0);
    }
}
