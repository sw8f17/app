package rocks.stalin.android.app.utils.time;

import android.support.annotation.NonNull;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by delusional on 5/4/17.
 */

public class Clock {
    public static final long NANO_TO_MILLIS = 1000000;
    public static final int MILLIS_TO_SEC = 1000;

    private static long lastMillis = 0;
    private static long lastNanos = 0;
    private static ReentrantReadWriteLock timeLock = new ReentrantReadWriteLock();

    public static native Instant getTime();

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

    public static Instant fromNanos(long nanos) {
        timeLock.readLock().lock();
        long nanosSinceLast = nanos - lastNanos;
        long milli = (nanosSinceLast / NANO_TO_MILLIS) + lastMillis;
        long remainNano = (nanosSinceLast % NANO_TO_MILLIS);
        timeLock.readLock().unlock();
        return new Instant(milli, remainNano);
    }

    public static class Instant implements Comparable<Instant> {
        private long millis;
        private long nanos;

        public Instant(long millis, long nanos) {
            this.millis = millis;
            this.nanos = nanos;
        }

        public long getMillis() {
            return millis;
        }

        public long getNanos() {
            return nanos;
        }

        public Duration timeBetween(Instant o) {
            long newMillis = Math.abs(millis - o.getMillis());
            long newNanos = Math.abs(nanos - o.getNanos());
            return new Duration(newMillis, newNanos);
        }

        public Instant add(Duration expectedEnd) {
            long newNanos = nanos + expectedEnd.getNanos();
            long newMillis = millis + expectedEnd.getMillis() + (newNanos / NANO_TO_MILLIS);
            newNanos %= NANO_TO_MILLIS;
            return new Instant(newMillis, newNanos);
        }

        public boolean before(Instant o) {
            return compareTo(o) < 0;
        }

        @Override
        public int compareTo(@NonNull Instant o) {
            if (o.getMillis() != this.getMillis())
                return Long.compare(millis, o.getMillis());
            return Long.compare(nanos, o.getNanos());
        }

        @Override
        public String toString() {
            return "Instant{" +
                    "millis=" + millis +
                    ", nanos=" + nanos +
                    '}';
        }
    }

    public static class Duration {
        private long millis;
        private long nanos;

        public Duration(long seconds, long nanos) {
            this.millis = seconds;
            this.nanos = nanos;
        }

        public static Duration FromNanos(long nanos) {
            long millis = (nanos / NANO_TO_MILLIS);
            long remainNanos = (nanos % NANO_TO_MILLIS);
            return new Duration(millis, remainNanos);
        }

        public long inSeconds() {
            long millis = inMillis();
            if(millis % MILLIS_TO_SEC >= MILLIS_TO_SEC / 2)
                return (millis / MILLIS_TO_SEC) + 1;
            return (millis / MILLIS_TO_SEC);
        }

        public long inMillis() {
            long roundedMillis = millis;
            if(nanos >= NANO_TO_MILLIS/2)
                roundedMillis += 1;
            return roundedMillis;
        }

        public long inNanos() {
            return millis * NANO_TO_MILLIS + nanos;
        }

        public long getMillis() {
            return millis;
        }

        public long getNanos() {
            return nanos;
        }

        public Duration multiply(int times) {
            long newNanos = nanos * times;
            long newMillis = millis * times + (newNanos / NANO_TO_MILLIS);
            newNanos %= NANO_TO_MILLIS;
            return new Duration(newMillis, newNanos);
        }

        @Override
        public String toString() {
            return "Duration{" +
                    "millis=" + millis +
                    ", nanos=" + nanos +
                    '}';
        }
    }
}
