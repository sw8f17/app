package rocks.stalin.android.app.framework.concurrent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by delusional on 5/12/17.
 */

public interface TaskScheduler extends TaskExecutor {
    ScheduledFuture<?> submitWithFixedRate(Runnable runnable, long time, TimeUnit unit);
    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);
}
