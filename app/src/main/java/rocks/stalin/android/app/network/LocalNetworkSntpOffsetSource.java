package rocks.stalin.android.app.network;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rocks.stalin.android.app.AverageWindow;
import rocks.stalin.android.app.framework.concurrent.TaskScheduler;
import rocks.stalin.android.app.proto.SntpRequest;
import rocks.stalin.android.app.proto.SntpResponse;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

public class LocalNetworkSntpOffsetSource implements OffsetSource, Runnable {
    private static final String TAG = LogHelper.makeLogTag(LocalNetworkSntpOffsetSource.class);
    private static final int AVERAGE_SIZE = 10;

    private MessageConnection connection;
    private TaskScheduler scheduler;
    private ScheduledFuture<?> future;

    private AverageWindow<Clock.Duration> window;

    private boolean running = false;

    public LocalNetworkSntpOffsetSource(MessageConnection connection, TaskScheduler scheduler) {
        this.connection = connection;
        this.scheduler = scheduler;

        window = new AverageWindow<>(new Clock.Duration(0, 0), AVERAGE_SIZE);
    }

    @Override
    public Clock.Duration getOffset() {
        return window.getAverage();
    }

    private void refreshOffset() {
        Clock.Instant receivedTime = Clock.getTime();
        Timestamp sentAt = new Timestamp.Builder()
                .millis(receivedTime.getMillis())
                .nanos(receivedTime.getNanos())
                .build();

        SntpRequest request = new SntpRequest.Builder().requestSent(sentAt).build();
        try {
            connection.send(request, SntpRequest.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            refreshOffset();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        connection.addHandler(SntpResponse.class, new MessageConnection.MessageListener<SntpResponse, SntpResponse.Builder>() {
            @Override
            public void packetReceived(SntpResponse message) {
                Clock.Instant T3 = Clock.getTime();
                Clock.Instant T2 = new Clock.Instant(message.responseSent.millis, message.responseSent.nanos);

                Clock.Instant T1 = new Clock.Instant(message.requestReceived.millis, message.requestReceived.nanos);
                Clock.Instant T0 = new Clock.Instant(message.requestSent.millis, message.requestSent.nanos);

                // Sanity check, throw error if the clocks resync in an unfortunate manner while we sync
                if(T0.sub(T3).shorterThan(T1.sub(T2))) {
                    LogHelper.w(TAG, "Bad sync timestamps");
                    return;
                }

                // ((T1 - T0) + (T2 - T3)) / 2
                try {
                    Clock.Duration requestOffset = T1.sub(T0);
                    Clock.Duration responseOffset = T2.sub(T3);
                    Clock.Duration sum = requestOffset.add(responseOffset);
                    Clock.Duration latestOffset = sum.divide(2);
                    window.putValue(latestOffset);
                    LogHelper.i(TAG, "The difference between the times were, ", requestOffset.sub(responseOffset));
                    LogHelper.i(TAG, "Time correction: ", latestOffset);
                    LogHelper.d(TAG, "Average time correction: ", window.getAverage());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        });

        future = scheduler.submitWithFixedRate(this, 1500, TimeUnit.MILLISECONDS);
        LogHelper.i(TAG, "Started time synchronization");

        running = true;
    }

    @Override
    public void stop() {
        future.cancel(false);
        connection.removeHandler(SntpResponse.class);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
