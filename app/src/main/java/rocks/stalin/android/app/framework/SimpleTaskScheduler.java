package rocks.stalin.android.app.framework;

import android.support.annotation.NonNull;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import rocks.stalin.android.app.framework.concurrent.TaskScheduler;

/**
 * Created by delusional on 5/12/17.
 */

public class SimpleTaskScheduler implements TaskScheduler {
    private ScheduledThreadPoolExecutor executor;
    private int threadNumber;

    /**
     * Create a new SimpleTaskScheduler with a factory setting the thread name according to the
     * template. The template is provided a single format int parameter (the thread count).
     * The thread is marked as a daemon thread by default.
     * @param poolSize The number of threads in the pool
     * @param threadNameTemplate The template of the thread name, optionally using a single
     *                           %d format parameter. Because it's formatted, please don't use
     *                           any user controllable input as the template.
     */
    public SimpleTaskScheduler(int poolSize, final String threadNameTemplate) {
        threadNumber = 0;
        executor = new ScheduledThreadPoolExecutor(poolSize, new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread t = new Thread(r);
                t.setName(String.format(threadNameTemplate, threadNumber++));
                t.setDaemon(true);
                return t;
            }
        });
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return executor.submit(runnable);
    }

    @Override
    public ScheduledFuture<?> submitWithFixedRate(Runnable runnable, long time, TimeUnit unit) {
        return executor.scheduleAtFixedRate(runnable, 0, time, unit);
    }
}
