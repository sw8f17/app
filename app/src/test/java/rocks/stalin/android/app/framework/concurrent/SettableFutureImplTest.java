package rocks.stalin.android.app.framework.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import rocks.stalin.android.app.framework.functional.Consumer;
import rocks.stalin.android.app.utils.time.Clock;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class SettableFutureImplTest {
    TaskExecutor executor = new SimpleTaskScheduler(10, "TEST_POOL-%d");

    private static class IntendedException extends RuntimeException {
    }

    private void failInTime(final SettableFuture<Integer> future, final CountDownLatch afterLatch) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(20);
                        break;
                    } catch (InterruptedException e) {
                    }
                }
                future.setException(new IntendedException());
                afterLatch.countDown();
            }
        });
    }

    private void postInTime(final SettableFuture<Integer> future, final CountDownLatch afterLatch) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(20);
                        break;
                    } catch (InterruptedException e) {
                    }
                }
                future.set(10);
                afterLatch.countDown();
            }
        });
    }

    @Test
    public void getAfterSetValue() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        future.set(10);

        Integer value = future.get();

        Assert.assertThat(value, equalTo(10));
    }

    @Test(expected = IntendedException.class)
    public void getAfterSetException() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        future.setException(new IntendedException());

        future.get();

        Assert.fail();
    }

    @Test
    public void getBeforeSetValue() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        CountDownLatch afterLatch =  new CountDownLatch(1);
        postInTime(future, afterLatch);

        Integer value = future.get();

        Assert.assertThat(value, equalTo(10));
    }

    @Test(expected = IntendedException.class)
    public void getBeforeSetException() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        CountDownLatch afterLatch =  new CountDownLatch(1);
        failInTime(future, afterLatch);

        future.get();

        Assert.fail();
    }

    @Test
    public void callbackBeforeSetValue() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        CountDownLatch afterLatch =  new CountDownLatch(1);
        postInTime(future, afterLatch);

        final boolean[] success = {false};

        future.setListener(new Consumer<Integer>() {
            @Override
            public void call(Integer value) {
                if (value == 10)
                    success[0] = true;
            }
        }, new Consumer<Throwable>() {
            @Override
            public void call(Throwable value) {
                success[0] = false;
            }
        });

        afterLatch.await();

        Assert.assertThat(success[0], equalTo(true));
    }

    @Test
    public void callbackBeforeSetException() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        CountDownLatch afterLatch =  new CountDownLatch(1);
        failInTime(future, afterLatch);

        final boolean[] success = {false};

        future.setListener(new Consumer<Integer>() {
            @Override
            public void call(Integer value) {
                success[0] = false;
            }
        }, new Consumer<Throwable>() {
            @Override
            public void call(Throwable value) {
                if(value instanceof IntendedException)
                    success[0] = true;
            }
        });

        afterLatch.await();

        Assert.assertThat(success[0], equalTo(true));
    }

    @Test
    public void callbackAfterSetValue() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        future.set(10);

        final boolean[] success = {false};

        future.setListener(new Consumer<Integer>() {
            @Override
            public void call(Integer value) {
                if (value == 10)
                    success[0] = true;
            }
        }, new Consumer<Throwable>() {
            @Override
            public void call(Throwable value) {
                success[0] = false;
            }
        });

        Assert.assertThat(success[0], equalTo(true));
    }

    @Test
    public void callbackAfterSetException() throws Exception {
        SettableFuture<Integer> future = new SettableFutureImpl<>();
        future.setException(new IntendedException());

        final boolean[] success = {false};

        future.setListener(new Consumer<Integer>() {
            @Override
            public void call(Integer value) {
                success[0] = false;
            }
        }, new Consumer<Throwable>() {
            @Override
            public void call(Throwable value) {
                if(value instanceof IntendedException)
                    success[0] = true;
            }
        });

        Assert.assertThat(success[0], equalTo(true));
    }
}