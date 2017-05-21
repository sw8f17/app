package rocks.stalin.android.app.framework.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by delusional on 5/12/17.
 */

public class TimeAwareTaskExecutor implements TaskExecutor {

    private final TaskExecutor shortTermExecutor;
    private final TaskExecutor longTermExecutor;

    public TimeAwareTaskExecutor(TaskExecutor shortTermExecutor, TaskExecutor longTermExecutor) {
        this.shortTermExecutor = shortTermExecutor;
        this.longTermExecutor = longTermExecutor;
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        if(runnable instanceof TimeAwareRunnable) {
            TimeAwareRunnable awareLifecycle = (TimeAwareRunnable) runnable;

            if(awareLifecycle.isLongRunning()) {
                return longTermExecutor.submit(runnable);
            }
        }
        return shortTermExecutor.submit(runnable);
    }
}
