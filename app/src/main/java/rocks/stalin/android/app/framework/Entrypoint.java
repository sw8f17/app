package rocks.stalin.android.app.framework;

import android.app.Application;

import rocks.stalin.android.app.framework.concurrent.CachedTaskExecutor;
import rocks.stalin.android.app.framework.concurrent.SimpleTaskScheduler;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.TimeAwareTaskExecutor;
import rocks.stalin.android.app.network.LocalNetworkSntpOffsetSourceFactory;
import rocks.stalin.android.app.network.OffsetSourceFactory;

/**
 * Created by delusional on 5/13/17.
 */

public class Entrypoint extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SimpleTaskScheduler shortTermExecutor = new SimpleTaskScheduler(10, "POOL-%d");
        TimeAwareTaskExecutor executorService = new TimeAwareTaskExecutor(shortTermExecutor, new CachedTaskExecutor());

        ServiceLocator.getInstance().putService(TaskExecutor.class, executorService);
        LocalNetworkSntpOffsetSourceFactory offsetSourceFactory = new LocalNetworkSntpOffsetSourceFactory(executorService, shortTermExecutor);
        ServiceLocator.getInstance().putService(OffsetSourceFactory.class, offsetSourceFactory);
    }
}
