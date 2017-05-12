package rocks.stalin.android.app.network;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rocks.stalin.android.app.proto.SntpRequest;
import rocks.stalin.android.app.proto.SntpResponse;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

public class LocalNetworkSntpOffsetSource implements OffsetSource {
    private static final String TAG = LogHelper.makeLogTag(LocalNetworkSntpOffsetSource.class);

    private MessageConnection connection;
    private Clock.Duration latestOffset = new Clock.Duration(0L, 0);

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> taskHandle;

    public LocalNetworkSntpOffsetSource(MessageConnection connection) {
        this.connection = connection;
        connection.addHandler(SntpResponse.class, new MessageConnection.MessageListener<SntpResponse, SntpResponse.Builder>() {
            @Override
            public void packetReceived(SntpResponse message) {
                LogHelper.i(TAG, "Sntp response received!!!");
                Clock.Instant requestSent = new Clock.Instant(message.requestSent.millis, message.requestSent.nanos);
                Clock.Instant requestReceived = new Clock.Instant(message.requestReceived.millis, message.requestReceived.nanos);
                Clock.Instant responseSent = new Clock.Instant(message.responseSent.millis, message.responseSent.nanos);
                Clock.Instant responseReceived = Clock.getTime();

                // ((T1 - T0) + (T2 - T3)) / 2
                Clock.Duration requestOffset = requestSent.timeBetween(requestReceived);
                Clock.Duration responseOffset = responseSent.timeBetween(responseReceived);
                Clock.Duration sum = requestOffset.add(responseOffset);
                latestOffset = sum.divide(2);
                LogHelper.i(TAG, "New sntp offset: ", latestOffset);
            }
        });

        taskHandle = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    refreshOffset();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public Clock.Duration getOffset() {
        return latestOffset;
    }

    @Override
    public void tearDown() {
        taskHandle.cancel(true);
        scheduler.shutdown();
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
}
