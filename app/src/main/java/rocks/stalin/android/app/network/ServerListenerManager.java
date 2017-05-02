package rocks.stalin.android.app.network;

import com.squareup.wire.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedHashMap;

import rocks.stalin.android.app.utils.LogHelper;


public class ServerListenerManager {
    private static final String TAG = LogHelper.makeLogTag(ServerListenerManager.class);
    private ServerListener listener;

    private LinkedHashMap<Socket, MessageConnection> processThreads = new LinkedHashMap<>();

    public void start(int port, final ClientListener clientListener) throws IOException {
        listener = new ServerListener(port);
        listener.start(new ServerListener.NewSocketListener() {
            @Override
            public void onNewSocket(final Socket socket) {
                MessageConnection dispatcher = new MessageConnection(socket);
                clientListener.connected(dispatcher);
                dispatcher.start();
                processThreads.put(socket, dispatcher);
            }

            @Override
            public <M extends Message<M, B>, B extends Message.Builder<M, B>> void sendProto(MessageConnection connection, M packet, Class<M> clazz) {

            }
        });
    }


    public void stop() throws IOException {
        listener.stop();
        for (MessageConnection m : processThreads.values()) {
            m.stop();
        }
    }

    public interface ClientListener {
        void connected(MessageConnection connection);

        <M extends Message<M, B>, B extends Message.Builder<M, B>> void sendProto(MessageConnection connection, M packet, Class<M> clazz);
    }
}
