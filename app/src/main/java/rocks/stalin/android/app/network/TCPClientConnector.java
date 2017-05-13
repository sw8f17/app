package rocks.stalin.android.app.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/12/17.
 */

public class TCPClientConnector implements Runnable {
    private static final String TAG = LogHelper.makeLogTag(TCPClientConnector.class);
    private final TaskExecutor executor;

    private final InetAddress host;
    private final int port;

    private ConnectionListener listener;

    public TCPClientConnector(InetAddress host, int port, TaskExecutor executor) {
        this.host = host;
        this.port = port;
        this.executor = executor;
    }

    public void setListener(ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        if(this.listener == null)
            throw new IllegalStateException("Listener not set");

        MessageConnection connection;
        try {
            Socket socket = new Socket(host, port);
            connection = new MessageConnection(socket, executor);
            connection.start();
        } catch (IOException e) {
            LogHelper.e(TAG, "Connection failed");
            return;
        }
        listener.onConnection(connection);
    }

    public interface ConnectionListener {
        void onConnection(MessageConnection connection);
    }
}
