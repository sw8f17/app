package rocks.stalin.android.app.framework.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import rocks.stalin.android.app.playback.MediaPlayerBackend;

/**
 * Created by delusional on 5/12/17.
 */

public interface TaskExecutor {
    Future<?> submit(Runnable runnable);
}
