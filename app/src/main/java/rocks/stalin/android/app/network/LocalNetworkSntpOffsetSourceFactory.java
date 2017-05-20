package rocks.stalin.android.app.network;

import java.util.concurrent.Callable;

import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFutureTask;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.TaskScheduler;

public class LocalNetworkSntpOffsetSourceFactory implements OffsetSourceFactory {

    private TaskExecutor executor;
    private TaskScheduler scheduler;

    public LocalNetworkSntpOffsetSourceFactory(TaskExecutor executor, TaskScheduler scheduler) {
        this.executor = executor;
        this.scheduler = scheduler;
    }

    public ObservableFuture<LocalNetworkSntpOffsetSource> create(final MessageConnection connection) {
        ObservableFutureTask<LocalNetworkSntpOffsetSource> future = new ObservableFutureTask<>(new Callable<LocalNetworkSntpOffsetSource>() {
            @Override
            public LocalNetworkSntpOffsetSource call() throws Exception {
                return new LocalNetworkSntpOffsetSource(connection, scheduler);
            }
        });

        executor.submit(future);

        return future;
    }
}
