package rocks.stalin.android.app.framework.concurrent;

import java.util.concurrent.locks.Lock;

public class ScopedLock implements AutoCloseable {
    private Lock lock;

    public ScopedLock(Lock lock) {
        this.lock = lock;
        lock.lock();
    }

    @Override
    public void close() {
        lock.unlock();
    }
}
