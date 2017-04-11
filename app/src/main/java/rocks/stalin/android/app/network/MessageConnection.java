package rocks.stalin.android.app.network;

import com.squareup.wire.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;

import static android.R.id.message;

public class MessageConnection {
    private static final String TAG = LogHelper.makeLogTag(MessageConnection.class);

    private Socket socket;
    private boolean running;
    private Thread processThread;

    private Map<Integer, MessageListener> handlers = new HashMap<>();

    public MessageConnection(Socket socket) {
        this.socket = socket;
    }

    public void start() {
        running = true;
        processThread = new Thread() {
            @Override
            public void run() {
                while(running) {
                    try {
                        InputStream stream = socket.getInputStream();
                        int type = stream.read();
                        int length = stream.read();
                        byte[] data = new byte[length];
                        stream.read(data, 0, length);
                        LogHelper.d(TAG, "Message retrieved");

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
        }
        if(handler != null && message != null) {
            handler.packetReceived(message);
        }
    }

    public <M extends Message<M, B>, B extends Message.Builder<M, B>> void send(M packet, Class<M> clazz) throws IOException {
        byte[] packetData = packet.adapter().encode(packet);
        OutputStream stream = socket.getOutputStream();
        stream.write(MessageRegistry.getInstance().getID(clazz));
        stream.write(packetData.length);
        stream.write(packetData);
    }

    public interface MessageListener<M extends Message<M, B>, B extends Message.Builder<M, B>> {
        @SuppressWarnings("deserialize")
        void packetReceived(M message);
    }
}
