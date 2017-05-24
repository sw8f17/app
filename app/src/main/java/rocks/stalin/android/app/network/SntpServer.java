package rocks.stalin.android.app.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.proto.SntpRequest;
import rocks.stalin.android.app.proto.SntpResponse;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;


public class SntpServer implements Lifecycle, Runnable {
    public final String TAG = LogHelper.makeLogTag(SntpServer.class);
    private final TaskExecutor executor;

    private SocketAddress address;
    private boolean running;

    private DatagramSocket socket;

    public SntpServer(SocketAddress address, TaskExecutor executor) {
        this.address = address;
        this.executor = executor;
    }

    public void register(final MessageConnection connection) {
        connection.addHandler(SntpRequest.class, new MessageConnection.MessageListener<SntpRequest, SntpRequest.Builder>() {
            @Override
            public void packetReceived(SntpRequest message) {
                LogHelper.i(TAG, "SntpRequest recieved");

            }
        });
    }

    @Override
    public void run() {
        byte[] data = new byte[(Long.SIZE + Integer.SIZE) * 3];
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        while(isRunning()) {
            try {
                DatagramPacket recvPacket = new DatagramPacket(data, 12);
                socket.receive(recvPacket);
                dataBuffer.limit(12);
                //Implicit buffer flip
                Clock.Instant receivedTime = Clock.getTime();

                Clock.Instant reqSentTime = getTime(dataBuffer);

                dataBuffer.clear();
                LogHelper.i(TAG, recvPacket.getSocketAddress());
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, recvPacket.getSocketAddress());

                putTime(dataBuffer, reqSentTime);
                putTime(dataBuffer, receivedTime);
                putTime(dataBuffer, Clock.getTime());
                //Correctness states we should: dataBuffer.flip();
                //But we wont, because we don't need to
                socket.send(sendPacket);
                dataBuffer.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    public void start() {
        try {
            socket = new DatagramSocket(address);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        running = true;
        executor.submit(this);
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
