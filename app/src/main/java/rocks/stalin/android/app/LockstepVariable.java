package rocks.stalin.android.app;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by delusional on 4/25/17.
 */

public class LockstepVariable<T> {
    private T value;

    private Lock lock = new ReentrantLock();
    private boolean lastWasSet = false;

    private Condition toNullCondition = lock.newCondition();

    private Condition fromNullCondition = lock.newCondition();

    public LockstepVariable(T value) {
        this.value = value;
    }

    public void set(T newValue) {
        lock.lock();
        while(lastWasSet) {
            try {
                toNullCondition.await();
            } catch (InterruptedException ignored) {
            }
        }

        value = newValue;
        lastWasSet = true;

        fromNullCondition.signalAll();
        lock.unlock();
    }

    public T get() {
        lock.lock();
        while(!lastWasSet) {
            try {
                fromNullCondition.await();
            } catch (InterruptedException ignored) {
            }
        }

        T oldValue =  value;
        lastWasSet = false;

        toNullCondition.signalAll();
        lock.unlock();

        return oldValue;
    }
}
