package rocks.stalin.android.app;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import rocks.stalin.android.app.network.WifiP2pServiceAnnouncer;

/**
 * Created by delusional on 4/5/17.
 */

public class ServerNetworkService extends Service {
    public static final String MODE_NAME = "MODE_NAME";
    public static final String MODE_SERVER = "MODE_SERVER";
    public static final String MODE_CLIENT = "MODE_CLIENT";

    public static final String CLIENT_HOST_NAME = "CLIENT_HOST_NAME";
    public static final String CLIENT_PORT_NAME = "CLIENT_PORT_NAME";

    private WifiP2pServiceAnnouncer server;

    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public void startServer() {
    }

    public class LocalBinder extends Binder {
        ServerNetworkService getService() {
            return ServerNetworkService.this;
        }
    }
}
