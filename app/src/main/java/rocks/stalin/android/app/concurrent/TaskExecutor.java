package rocks.stalin.android.app.concurrent;

import java.util.concurrent.Future;

/**
 * Created by delusional on 5/12/17.
 */

public interface TaskExecutor {
    Future<?> submit(Runnable runnable);
}
