package rocks.stalin.android.app.network;

import android.util.SparseArray;

import com.squareup.wire.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.TimeAwareRunnable;
import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.PauseCommand;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.proto.SeekCommand;
import rocks.stalin.android.app.proto.SessionInfo;
import rocks.stalin.android.app.proto.SntpRequest;
import rocks.stalin.android.app.proto.SntpResponse;
import rocks.stalin.android.app.proto.SongChangeCommand;
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
        while(running) {
            try {
                int type = dis.readByte();
                int length = dis.readInt();
                if(length < 0){
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
            case 2:
                message = Music.ADAPTER.decode(data);
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
            default:
                LogHelper.w(TAG, "Received unknown message of type ", type);
                return;
        }

        MessageListener handler = handlers.get(type);

        if(handler != null && message != null) {
            /*
             * This is unsafe, since it's deserializing the network data.
             */
            handler.packetReceived(message);
        }
    }

    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void send(M packet, Class<? extends M> clazz) throws IOException {
        byte[] packetData = packet.adapter().encode(packet);
        dos.writeByte(MessageRegistry.getInstance().getID(clazz));
        dos.writeInt(packetData.length);
        dos.write(packetData);
        dos.flush();
    }

    @Override
    public boolean isLongRunning() {
        return true;
    }

    public interface MessageListener<M extends Message<M, B>, B extends Message.Builder<M, B>> {
        @SuppressWarnings("deserialize")
        void packetReceived(M message);
    }
}
