package rocks.stalin.android.app;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;

import rocks.stalin.android.app.network.StreamerNetworkService;
import rocks.stalin.android.app.network.WifiDirectBroadcastReceiver;
import rocks.stalin.android.app.utils.LogHelper;

import static android.content.ContentValues.TAG;

/**
 * Created by delusional on 4/5/17.
 */

public class NetworkService extends Service {
    public static final String MODE_NAME = "MODE_NAME";
    public static final String MODE_SERVER = "MODE_SERVER";
    public static final String MODE_CLIENT = "MODE_SERVER";

    public static final String CLIENT_HOST_NAME = "CLIENT_HOST_NAME";
    public static final String CLIENT_PORT_NAME = "CLIENT_PORT_NAME";

    private StreamerNetworkService streamer;

    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        WifiP2pManager manager = getSystemService(WifiP2pManager.class);
        streamer = new StreamerNetworkService(manager);
        streamer.initialize(this);
    }

    public void startServer() {
        streamer.listen();
    }

    public void startClient(final String host, final int port) {
        streamer.connect(this, host, port);
    }

    public void stopClient() throws IOException {
        streamer.stopListen();
    }

    public class LocalBinder extends Binder {
        NetworkService getService() {
            return NetworkService.this;
        }
    }
}
