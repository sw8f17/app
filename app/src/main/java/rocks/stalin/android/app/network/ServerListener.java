package rocks.stalin.android.app.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by delusional on 4/4/17.
 */

public class ServerListener {
    private int port;
    private ServerSocket serverSocket;

    public ServerListener(int port) throws IOException {
        this.port = port;
    }

    public void Listen() throws IOException {
        serverSocket = new ServerSocket(port);
        while(true) {
            Socket client = serverSocket.accept();
            client.getOutputStream().write(10);
        }
    }
}
