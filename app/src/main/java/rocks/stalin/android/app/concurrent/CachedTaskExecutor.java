package rocks.stalin.android.app.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by delusional on 5/12/17.
 */

public class CachedTaskExecutor implements TaskExecutor {
    ExecutorService executor;

    public CachedTaskExecutor() {
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return executor.submit(runnable);
    }
}
