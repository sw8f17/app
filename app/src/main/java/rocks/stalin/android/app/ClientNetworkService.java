package rocks.stalin.android.app;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;

import rocks.stalin.android.app.network.WifiP2PMessageClient;

/**
 * Created by delusional on 4/13/17.
 */

public class ClientNetworkService extends Service{
    private WifiP2PMessageClient client;

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
        client = new WifiP2PMessageClient(manager);
        client.initialize(this);
    }

    public void startClient(final String host, final int port) {
        client.connect(this, host, port);
    }

    public void stopClient() throws IOException {
        client.disconnect();
    }

    public class LocalBinder extends Binder {
        ClientNetworkService getService() {
            return ClientNetworkService.this;
        }
    }
}
