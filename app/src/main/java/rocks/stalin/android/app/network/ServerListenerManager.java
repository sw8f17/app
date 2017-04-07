package rocks.stalin.android.app.network;

import com.squareup.wire.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rocks.stalin.android.app.ClientMusicService;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/4/17.
 */

public class ServerListenerManager {
    private static final String TAG = LogHelper.makeLogTag(ServerListenerManager.class);
    private ServerListener listener;

    private LinkedHashMap<Socket, Thread> processThreads = new LinkedHashMap<>();

    private Map<Integer, PacketListener> handlers = new HashMap<>();

    private boolean running;

    public ServerListener start(int port, final ClientListener clientListener) throws IOException {
        running = true;
        listener = new ServerListener(port);
        listener.start(new ServerListener.NewSocketListener() {
            @Override
            public void onNewSocket(final Socket socket) {
                clientListener.connected(socket);
                final Thread processThread = new Thread() {
                    @Override
                    public void run() {
                        while(running) {
                            try {
                                InputStream stream = socket.getInputStream();
                                int type = stream.read();
                                int length = stream.read();
                                byte[] data = new byte[length];
                                stream.read(data, 0, length);

                                processMessage(type, data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                processThread.start();
                processThreads.put(socket, processThread);
            }
        });
        return listener;
    }

    public <T extends Message> void addHandler(Class<T> messageType, PacketListener<T> listener) {
        handlers.put(MessageRegistry.getInstance().getID(messageType), listener);
    }

    private void processMessage(int type, byte[] data) throws IOException {
        switch (type) {
            case 1:
                Welcome message = Welcome.ADAPTER.decode(data);
                handlers.get(type).packetReceived(message);
        }
    }

    public void stop() throws IOException {
        running = false;
        listener.stop();
        for (Socket s : processThreads.keySet()) {
            s.close();
        }
    }

    public interface ClientListener {
        void connected(Socket socket);
    }

    public interface PacketListener<T extends Message> {
        void packetReceived(T message);
    }
}
