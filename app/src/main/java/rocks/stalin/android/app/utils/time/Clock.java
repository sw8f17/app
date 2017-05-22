package rocks.stalin.android.app.utils.time;

import android.support.annotation.NonNull;
import android.support.v7.view.menu.MenuItemImpl;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import rocks.stalin.android.app.framework.SimpleMath;

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
        int remainNano = (int) (nanosSinceLast % NANO_TO_MILLIS);
        timeLock.readLock().unlock();
        return new Instant(milli, remainNano);
    }

    public static class Instant implements Comparable<Instant> {
        private long millis;
        private int nanos;

        public Instant(long millis, int nanos) {
            if(millis < 0 || nanos < 0)
                throw new IllegalArgumentException("How do you expect me to deal with negative instants");
            this.millis = millis;
            if(nanos >= NANO_TO_MILLIS)
                throw new IllegalArgumentException("The nanos given was greater than one millisecond: " +  nanos);

            this.nanos = nanos;
        }

        /**
         * Rounds the instant to the nearest millis, 500.000 rounds up.
         * @return the instant as millis rounded.
         */
        public long inMillis() {
            long roundedMillis = millis;
            if(nanos >= NANO_TO_MILLIS/2)
                roundedMillis += 1;
            return roundedMillis;
        }

        public long getMillis() {
            return millis;
        }

        public int getNanos() {
            return nanos;
        }

        public Duration sub(Instant o) {
            long newMillis = millis - o.getMillis();
            int newNanos = nanos - o.getNanos();
            if(newNanos < 0 && newMillis > 0) {
                newMillis -= 1;
                newNanos += NANO_TO_MILLIS;
            } else if(newNanos > 0 && newMillis < 0) {
                newMillis += 1;
                newNanos -= NANO_TO_MILLIS;
            }
            return new Duration(newMillis, newNanos);
        }

        public Instant add(Duration expectedEnd) {
            long newNanos = nanos + expectedEnd.getNanos();
            long newMillis = millis + expectedEnd.getMillis() + (newNanos / NANO_TO_MILLIS);
            newNanos %= NANO_TO_MILLIS;
            if(newNanos < 0 && newMillis > 0) {
                newMillis -= 1;
                newNanos += NANO_TO_MILLIS;
            } else if(newNanos > 0 && newMillis < 0) {
                newMillis += 1;
                newNanos -= NANO_TO_MILLIS;
            }
            return new Instant(newMillis, (int) newNanos);
        }

        public Instant sub(Duration expectedEnd) {
            long newNanos = nanos - expectedEnd.getNanos();
            long newMillis = millis - expectedEnd.getMillis();
            if(newNanos < 0) {
                newMillis -= 1;
                newNanos = NANO_TO_MILLIS + newNanos;
            }
            newMillis += newNanos / NANO_TO_MILLIS;
            newNanos %= NANO_TO_MILLIS;
            return new Instant(newMillis, (int) newNanos);
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
            return millis + ":" + nanos;
        }
    }

    public static class Duration implements SimpleMath<Duration>{
        private long millis;
        private int nanos;

        public Duration(long millis, int nanos) {
            this.millis = millis;
            if(nanos / NANO_TO_MILLIS != 0)
                throw new IllegalArgumentException("Durations have to be normalized");
            if(millis > 0 && nanos < 0)
                throw new IllegalArgumentException("Inconsistent signs");
            if(millis < 0 && nanos > 0)
                throw new IllegalArgumentException("Inconsistent signs");

            this.nanos = nanos;
        }

        public static Duration fromSeconds(long seconds) {
            long millis = seconds * MILLIS_TO_SEC;
            return fromMillis(millis);
        }

        public static Duration fromMillis(long millis) {
            return new Duration(millis, 0);
        }

        public static Duration FromNanos(long nanos) {
            long millis = (nanos / NANO_TO_MILLIS);
            int remainNanos = (int) (nanos % NANO_TO_MILLIS);
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
            else if(nanos < -NANO_TO_MILLIS/2)
                roundedMillis -= 1;
            return roundedMillis;
        }

        @Override
        public Duration add(Duration operand) {
            int newNanos = this.nanos + operand.nanos;
            long newMillis = this.millis + operand.millis;
            newMillis += newNanos / NANO_TO_MILLIS;
            newNanos %= NANO_TO_MILLIS;
            if(newNanos < 0 && newMillis > 0) {
                newMillis -= 1;
                newNanos += NANO_TO_MILLIS;
            } else if(newNanos > 0 && newMillis < 0) {
                newMillis += 1;
                newNanos -= NANO_TO_MILLIS;
            }

            return new Duration(newMillis, newNanos);
        }

        @Override
        public Duration sub(Duration operand) {
            long newNanos = nanos - operand.getNanos();
            long newMillis = millis - operand.getMillis();
            if(newNanos < 0) {
                newMillis -= 1;
                newNanos = NANO_TO_MILLIS + newNanos;
            }
            newMillis += newNanos / NANO_TO_MILLIS;
            newNanos %= NANO_TO_MILLIS;
            return new Duration(newMillis, (int) newNanos);
        }

        public long inNanos() {
            return millis * NANO_TO_MILLIS + nanos;
        }

        public long getMillis() {
            return millis;
        }

        public int getNanos() {
            return nanos;
        }

        @Override
        public Duration multiply(int times) {
            long newNanos = nanos * times;
            long newMillis = millis * times + (newNanos / NANO_TO_MILLIS);
            newNanos %= NANO_TO_MILLIS;
            return new Duration(newMillis, (int) newNanos);
        }

        @Override
        public Duration divide(int denominator) {
            long newMillis = this.millis / denominator;

            // There might be a remainder from the millis division, convert to nanos and try again.
            long remainderMillis = this.millis % denominator;
            long newNanos = (this.nanos + remainderMillis * NANO_TO_MILLIS) / denominator;

            return new Duration(newMillis, (int) newNanos);
        }

        public boolean shorterThan(Duration other) {
            long otherMillisAbs = Math.abs(other.getMillis());
            long thisMillisAbs = Math.abs(getMillis());
            return thisMillisAbs == otherMillisAbs ? Math.abs(getNanos()) < Math.abs(other.getNanos()) : thisMillisAbs < otherMillisAbs;

        }

        @Override
        public String toString() {
            return millis + ":" + nanos + "D";
        }
    }
}
