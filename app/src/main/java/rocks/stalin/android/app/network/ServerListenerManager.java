package rocks.stalin.android.app.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/4/17.
 */

public class ServerListenerManager {
    private static final String TAG = LogHelper.makeLogTag(ServerListenerManager.class);
    private Map<ServerListener, Thread> listenThreads = new HashMap<>();

    public ServerListener listen(final int port) throws IOException {
        final ServerListener listener = new ServerListener(port);
        Thread lThread = new Thread() {
            @Override
            public void run() {
                try {
                    listener.Listen();
                } catch (IOException e) {
                    LogHelper.e(TAG, "Error listening on port: ", port);
                }
            }
        };
        listenThreads.put(listener, lThread);
        lThread.run();
        return listener;
    }
}
