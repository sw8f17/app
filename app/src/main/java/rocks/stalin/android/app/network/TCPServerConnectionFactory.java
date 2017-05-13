package rocks.stalin.android.app.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import rocks.stalin.android.app.framework.concurrent.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.TimeAwareRunnable;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/12/17.
 */

public class TCPServerConnectionFactory implements Lifecycle, TimeAwareRunnable {
    private static final String TAG = LogHelper.makeLogTag(TCPServerConnectionFactory.class);

    private int port;

    private TaskExecutor executorService;

    private boolean running;
    private ServerSocket serverSocket;
    private List<MessageConnection> connections;

    private boolean listening;

    private NewConnectionListener listener;

    public TCPServerConnectionFactory(int port, TaskExecutor executorService) {
        this.port = port;
        this.executorService = executorService;

        connections = new ArrayList<>();
        listening = false;
    }

    public void setListener(NewConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            listening = true;
        } catch (IOException e) {
            LogHelper.e(TAG, "Failed binding serversocket to ", port);
            throw new RuntimeException(e);
        }

        try {
            while (true) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    LogHelper.i(TAG, "Server socket timed out, no matter");
                    continue;
                }

                MessageConnection connection = new MessageConnection(socket, executorService);
                connections.add(connection);
                listener.onNewConnection(connection);
                //TODO: Submit to executor
                connection.start();
            }
        } catch (SocketException e) {
            if(listening && !running)
                LogHelper.i(TAG, "Socket closed");
            LogHelper.e(TAG, "Error on socket server ", port, " Stacktrace: ", e);
        } catch (Exception e) {
            LogHelper.e(TAG, "Error on socket server ", port, " Stacktrace: ", e);
        } finally {
            listening = false;
        }
    }

    @Override
    public void start() {
        if(listener == null) {
            LogHelper.e(TAG, "No listener registered");
            throw new RuntimeException("No listener registered");
        }

        running = true;
        executorService.submit(this);
    }

    @Override
    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        serverSocket = null;
        running = false;
        for(MessageConnection connection : connections)
            connection.stop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isLongRunning() {
        return true;
    }

    /**
     * Listener for new connection
     * Please keep the callback quick, we can't accept anyone before it returns.
     */
    public interface NewConnectionListener {
        void onNewConnection(MessageConnection connection);
    }
}
