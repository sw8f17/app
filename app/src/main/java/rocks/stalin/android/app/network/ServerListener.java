package rocks.stalin.android.app.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import rocks.stalin.android.app.utils.LogHelper;

public class ServerListener {
    private static final String TAG = LogHelper.makeLogTag(ServerListener.class);

    private int port;
    private boolean running;
    private ServerSocket serverSocket;
    private Thread thread;

    public ServerListener(int port) throws IOException {
        this.port = port;
    }

    public void start(final NewSocketListener listener) throws IOException {
        running = true;
        SocketAddress address = new InetSocketAddress("0.0.0.0", port);
        serverSocket = new ServerSocket();
        serverSocket.bind(address);

        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (running) {
                        Socket client = serverSocket.accept();
                        listener.onNewSocket(client);
                    }
                } catch (IOException e) {
                    LogHelper.e(TAG, "Error listening on port: ", port);
                }
            }
        };

        thread.start();
    }

    public void stop() throws IOException {
        running = false;
        thread.interrupt();
        serverSocket.close();
    }

    public interface NewSocketListener {
        void onNewSocket(Socket socket);
    }
}
