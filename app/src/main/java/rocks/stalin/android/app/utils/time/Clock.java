package rocks.stalin.android.app.utils.time;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by delusional on 5/4/17.
 */

public class Clock {
    private static long lastMillis = 0;
    private static long lastNanos = 0;
    private static ReentrantReadWriteLock timeLock = new ReentrantReadWriteLock();

    public static native Time getTime();
    private static native void staticInit();


    static {
        System.loadLibrary("native-lib");
        staticInit();
        sync();
    }

    public static void sync() {
        timeLock.writeLock().lock();
        lastMillis = System.currentTimeMillis();
        lastNanos = System.nanoTime();
        timeLock.writeLock().unlock();
    }

    public static Time fromNanos(long nanos) {
        timeLock.readLock().lock();
        long nanosSinceLast = lastNanos - nanos;
        long milli      = (nanosSinceLast / 1000000) + lastMillis;
        long remainNano = (nanosSinceLast % 1000000);
        timeLock.readLock().unlock();
        return new Time(milli, remainNano);
    }

    public static class Time {
        private long seconds;
        private long nanos;

        public Time(long seconds, long nanos) {
            this.seconds = seconds;
            this.nanos = nanos;
        }
    }
}
