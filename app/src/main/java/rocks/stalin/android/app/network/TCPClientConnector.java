package rocks.stalin.android.app.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;

import rocks.stalin.android.app.framework.AsyncFactory;
import rocks.stalin.android.app.framework.concurrent.ObservableFuture;
import rocks.stalin.android.app.framework.concurrent.ObservableFutureTask;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/12/17.
 */

public class TCPClientConnector implements AsyncFactory<MessageConnection> {
    private static final String TAG = LogHelper.makeLogTag(TCPClientConnector.class);
    private final TaskExecutor executor;

    private final InetAddress host;
    private final int port;

    public TCPClientConnector(InetAddress host, int port, TaskExecutor executor) {
        this.host = host;
        this.port = port;
        this.executor = executor;
    }

    @Override
    public ObservableFuture<MessageConnection> create() {
        ObservableFutureTask<MessageConnection> futureTask = new ObservableFutureTask<>(new Callable<MessageConnection>() {
            @Override
            public MessageConnection call() throws Exception {
                MessageConnection connection;
                try {
                    Socket socket = new Socket(host, port);
                    connection = new MessageConnection(socket, executor);
                    connection.start();
                    return connection;
                } catch (IOException e) {
                    LogHelper.e(TAG, "Connection failed");
                    throw new RuntimeException(e);
                }
            }
        });
        executor.submit(futureTask);
        return futureTask;
    }

    public interface ConnectionListener {
        void onConnection(MessageConnection connection);
    }
}
