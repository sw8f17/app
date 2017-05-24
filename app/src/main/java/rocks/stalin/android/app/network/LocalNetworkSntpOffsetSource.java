package rocks.stalin.android.app.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
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
    private static final int AVERAGE_SIZE = 15;

    private final DatagramSocket socket;
    private final String remoteHost;
    private final int remotePort;

    private TaskScheduler scheduler;
    private ScheduledFuture<?> future;

    private AverageWindow<Clock.Duration> window;

    private boolean running = false;

    public LocalNetworkSntpOffsetSource(String localIp, int localPort, String remoteHost, int remotePort, TaskScheduler scheduler) throws SocketException {
        this.socket = new DatagramSocket(new InetSocketAddress(localIp, localPort));
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.scheduler = scheduler;

        window = new AverageWindow<>(new Clock.Duration(0, 0), AVERAGE_SIZE);
    }

    @Override
    public Clock.Duration getOffset() {
        return window.getAverage();
    }

    private void refreshOffset() {

        byte[] data = new byte[(Long.SIZE + Integer.SIZE) * 3];
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        try {
            Clock.Instant requestStartTime = Clock.getTime();

            putTime(dataBuffer, requestStartTime);
            //Implicit flip
            socket.send(new DatagramPacket(data, 12, new InetSocketAddress(remoteHost, remotePort)));
            dataBuffer.clear();

            DatagramPacket recvPacket = new DatagramPacket(data, data.length);
            socket.receive(recvPacket);
            dataBuffer.position(data.length); //Set the position to fit the receive
            dataBuffer.flip();

            Clock.Instant T3 = Clock.getTime();
            Clock.Instant T0 = getTime(dataBuffer);
            Clock.Instant T1 = getTime(dataBuffer);
            Clock.Instant T2 = getTime(dataBuffer);


            dataBuffer.clear();

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void putTime(ByteBuffer buffer, Clock.Instant receivedTime) {
        //Put the millis (long)
        buffer.putLong(receivedTime.getMillis());
        buffer.putInt(receivedTime.getNanos());
    }

    private Clock.Instant getTime(ByteBuffer buffer) {
        long millis = buffer.getLong();
        int nanos = buffer.getInt();
        return new Clock.Instant(millis, nanos);
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
        future = scheduler.submitWithFixedRate(this, 500, TimeUnit.MILLISECONDS);
        LogHelper.i(TAG, "Started time synchronization");

        running = true;
    }

    @Override
    public void stop() {
        future.cancel(false);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
