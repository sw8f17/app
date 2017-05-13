package rocks.stalin.android.app.framework.concurrent;

/**
 * Created by delusional on 5/12/17.
 */

public interface Lifecycle {
    void start();
    void stop();

    boolean isRunning();
}
