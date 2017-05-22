package rocks.stalin.android.app;

import java.lang.reflect.Array;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import rocks.stalin.android.app.framework.SimpleMath;

public class AverageWindow<T extends SimpleMath<T>> {
    private Queue<T> values;
    private T currentAvg;

    private int size;

    //Should be == size at all times except while "warming up"
    private int currentCount;

    public AverageWindow(Class<T> clazz, T initialValue, int size) {
        this.values = new LinkedBlockingQueue<>(size);
        this.size = size;
        this.currentCount = 0;
        this.currentAvg = initialValue;
    }

    public void putValue(T value) {
        if(currentCount == 0) {
            values.add(value);
            currentAvg = value;
        } else if (currentCount < size) {
            values.add(value);
            currentAvg = currentAvg.divide(2).add(value.divide(2));
        } else {
            T old = values.poll().divide(currentCount);
            T n = value.divide(currentCount);
            values.add(value);
            currentAvg = currentAvg.sub(old).add(n);
        }
        currentCount += currentCount == size ? 0 : 1;
    }

    public T getAverage() {
        return currentAvg;
    }
}
