package rocks.stalin.android.app.framework;

import android.app.Application;

import rocks.stalin.android.app.framework.concurrent.CachedTaskExecutor;
import rocks.stalin.android.app.framework.concurrent.SimpleTaskScheduler;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.TimeAwareTaskExecutor;

/**
 * Created by delusional on 5/13/17.
 */

public class Entrypoint extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        TimeAwareTaskExecutor executorService = new TimeAwareTaskExecutor(new SimpleTaskScheduler(10, "POOL-%d"), new CachedTaskExecutor());

        ServiceLocator.getInstance().putService(TaskExecutor.class, executorService);
    }
}
