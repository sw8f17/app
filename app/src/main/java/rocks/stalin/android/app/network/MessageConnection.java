package rocks.stalin.android.app.network;

import android.util.SparseArray;

import com.squareup.wire.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.TimeAwareRunnable;
import rocks.stalin.android.app.proto.NewMusic;
import rocks.stalin.android.app.proto.PauseCommand;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.proto.SeekCommand;
import rocks.stalin.android.app.proto.SessionInfo;
import rocks.stalin.android.app.proto.SntpRequest;
import rocks.stalin.android.app.proto.SntpResponse;
import rocks.stalin.android.app.proto.SongChangeCommand;
import rocks.stalin.android.app.proto.Sync;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;

public class MessageConnection implements Lifecycle, TimeAwareRunnable {
    private static final String TAG = LogHelper.makeLogTag(MessageConnection.class);
    private final TaskExecutor executorService;

    private Socket socket;

    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean running;

    private SparseArray<MessageListener> handlers = new SparseArray<>();
    private ReentrantLock connectionLock = new ReentrantLock();

    public MessageConnection(Socket socket, TaskExecutor executorService) {
        this.socket = socket;
        this.executorService = executorService;

        try {
            InputStream stream = socket.getInputStream();
            dis = new DataInputStream(stream);

            OutputStream ostream = socket.getOutputStream();
            dos = new DataOutputStream(ostream);
        } catch (IOException e) {
            LogHelper.e(TAG, "Failed creating I/O streams for the socket");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {
            while (running) {
                try {
                    int type = dis.readByte();
                    int length = dis.readInt();
                    if (length < 0) {
                        LogHelper.e(TAG, "We got a length of ", length, " which doesn't make any sense. The type was ", type, " by the way");
                    }
                    byte[] data = new byte[length];
                    dis.readFully(data, 0, length);

                    processMessage(type, data);
                } catch (IOException e) {
                    LogHelper.e(TAG, "Error reading from master device");
                    e.printStackTrace();
                    running = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        running = true;
        executorService.submit(this);
    }

    @Override
    public void stop() {
        running = false;

        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void addHandler(Class<M> messageType, MessageListener<M,B> listener) {
        handlers.put(MessageRegistry.getInstance().getID(messageType), listener);
    }

    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void removeHandler(Class<M> messageType) {
        handlers.delete(MessageRegistry.getInstance().getID(messageType));
    }

    private void processMessage(int type, byte[] data) throws IOException {
        Message<?, ?> message;
        switch (type) {
            case 1:
                message = Welcome.ADAPTER.decode(data);
                break;
            case 3:
                message = PlayCommand.ADAPTER.decode(data);
                break;
            case 4:
                message = PauseCommand.ADAPTER.decode(data);
                break;
            case 5:
                message = SeekCommand.ADAPTER.decode(data);
                break;
            case 6:
                message = SongChangeCommand.ADAPTER.decode(data);
                break;
            case 7:
                message = SessionInfo.ADAPTER.decode(data);
                break;
            case 8:
                message = SntpRequest.ADAPTER.decode(data);
                break;
            case 9:
                message = SntpResponse.ADAPTER.decode(data);
                break;
            case 10:
                message = NewMusic.ADAPTER.decode(data);
                break;
            case 11:
                message = Sync.ADAPTER.decode(data);
                break;
            default:
                LogHelper.w(TAG, "Received unknown message of type ", type);
                return;
        }

        MessageListener handler = handlers.get(type);

        if(handler == null) {
            LogHelper.w(TAG, "No handler registered for received message ", message.getClass().getName());
            return;
        }

        /*
         * This is unsafe, since it's deserializing the network data.
         */
        handler.packetReceived(message);
    }

    public void prepareSend() {
        connectionLock.lock();
    }

    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void send(M packet, Class<? extends M> clazz) throws IOException {
        if(!connectionLock.isHeldByCurrentThread())
            connectionLock.lock();
        try {
            byte[] packetData = packet.adapter().encode(packet);
            dos.writeByte(MessageRegistry.getInstance().getID(clazz));
            dos.writeInt(packetData.length);
            dos.write(packetData);
            dos.flush();
        }finally {
            connectionLock.unlock();
        }
    }

    @Override
    public boolean isLongRunning() {
        return true;
    }

    public interface MessageListener<M extends Message<M, B>, B extends Message.Builder<M, B>> {
        void packetReceived(M message);
    }
}
