package rocks.stalin.android.app.framework.concurrent;

/**
 * Created by delusional on 5/12/17.
 */

public interface TimeAwareRunnable extends Runnable {
    boolean isLongRunning();
}
