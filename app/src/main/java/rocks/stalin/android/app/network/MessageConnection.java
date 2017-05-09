package rocks.stalin.android.app.network;

import android.util.SparseArray;

import com.squareup.wire.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.PauseCommand;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.proto.SeekCommand;
import rocks.stalin.android.app.proto.SessionInfo;
import rocks.stalin.android.app.proto.SongChangeCommand;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;

public class MessageConnection {
    private static final String TAG = LogHelper.makeLogTag(MessageConnection.class);

    private Socket socket;
    DataInputStream dis;
    DataOutputStream dos;
    private boolean running;
    private Thread processThread;

    private SparseArray<MessageListener> handlers = new SparseArray<>();

    public MessageConnection(Socket socket) {
        this.socket = socket;
        try {
            InputStream stream = socket.getInputStream();
            dis = new DataInputStream(stream);

            OutputStream ostream = socket.getOutputStream();
            dos = new DataOutputStream(ostream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        running = true;
        processThread = new Thread() {
            @Override
            public void run() {
                while(running) {
                    try {
                        int type = dis.readByte();
                        int length = dis.readInt();
                        byte[] data = new byte[length];
                        dis.readFully(data, 0, length);

                        processMessage(type, data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        processThread.start();
    }

    public void stop() throws IOException {
        running = false;
        socket.close();
    }

    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void addHandler(Class<M> messageType, MessageListener<M,B> listener) {
        handlers.put(MessageRegistry.getInstance().getID(messageType), listener);
    }

    private void processMessage(int type, byte[] data) throws IOException {
        Message<?, ?> message = null;
        MessageListener handler = null;
        switch (type) {
            case 1:
                message = Welcome.ADAPTER.decode(data);
                handler = handlers.get(type);
                break;
            case 2:
                message = Music.ADAPTER.decode(data);
                handler = handlers.get(type);
                break;
            case 3:
                message = PlayCommand.ADAPTER.decode(data);
                handler = handlers.get(type);
                break;
            case 4:
                message = PauseCommand.ADAPTER.decode(data);
                handler = handlers.get(type);
                break;
            case 5:
                message = SeekCommand.ADAPTER.decode(data);
                handler = handlers.get(type);
                break;
            case 6:
                message = SongChangeCommand.ADAPTER.decode(data);
                handler = handlers.get(type);
                break;
            case 7:
                message = SessionInfo.ADAPTER.decode(data);
                handler = handlers.get(type);
                break;
            default:
                LogHelper.w(TAG, "Received unknown message of type ", type);
                return;
        }
        if(handler != null && message != null) {
            /**
             * This is unsafe, since it's deserializing the network data.
             */
            handler.packetReceived(message);
        }
    }

    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void send(M packet, Class<M> clazz) throws IOException {
        byte[] packetData = packet.adapter().encode(packet);
        dos.writeByte(MessageRegistry.getInstance().getID(clazz));
        dos.writeInt(packetData.length);
        dos.write(packetData);
        dos.flush();
    }

    public interface MessageListener<M extends Message<M, B>, B extends Message.Builder<M, B>> {
        @SuppressWarnings("deserialize")
        void packetReceived(M message);
    }
}
