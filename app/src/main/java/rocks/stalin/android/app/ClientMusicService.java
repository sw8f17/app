package rocks.stalin.android.app;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;

import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/6/17.
 */

public class ClientMusicService extends Service {
    private static final String TAG = LogHelper.makeLogTag(ClientMusicService.class);

    public static final String ACTION_CONNECT = "rocks.stalin.android.app.ACTION_CONNECT";
    public static final String ACTION_STOP = "rocks.stalin.android.app.ACTION_STOP";

    public static final String CONNECT_HOST_NAME = "CONNECT_HOST_NAME";
    public static final String CONNECT_PORT_NAME = "CONNECT_PORT_NAME";

    private boolean bound = false;
    private ClientNetworkService network;

    private boolean connected;
    private String hostname;
    private int port;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogHelper.i(TAG, "Network service bound");
            ClientNetworkService.LocalBinder binder = (ClientNetworkService.LocalBinder) iBinder;
            network = binder.getService();
            bound = true;

            if (!connected)
                network.startClient(hostname, port);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogHelper.i(TAG, "Network service unbound");
            if (connected) {
                try {
                    network.stopClient();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            bound = false;
            network = null;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_CONNECT)) {
            LogHelper.i(TAG, "Creating music service");

            Intent i = new Intent(this, ClientNetworkService.class);
            bound = bindService(i, conn, BIND_AUTO_CREATE);

            hostname = intent.getStringExtra(CONNECT_HOST_NAME);
            port = intent.getIntExtra(CONNECT_PORT_NAME, -1);

            //TODO: I don't think this will ever happen
            if(bound && !connected) {
                connected = true;
                network.startClient(hostname, port);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bound)
            unbindService(conn);
        Intent i = new Intent(this, ServerNetworkService.class);
        stopService(i);
    }
}
