package rocks.stalin.android.app.network;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

import rocks.stalin.android.app.framework.concurrent.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/13/17.
 */

public class WifiP2PConnection implements Lifecycle, TCPClientConnector.ConnectionListener {
    private static final String TAG = LogHelper.makeLogTag(WifiP2PConnection.class);

    private Context context;
    private final WifiP2PManagerFacade manager;
    private final String host;
    private int port;
    private TaskExecutor executor;

    private boolean running = false;

    private ConnectedListener listener;

    WifiDirectBroadcastReceiver rec;

    public WifiP2PConnection(Context context, WifiP2PManagerFacade manager, String host, int port, TaskExecutor executor) {
        this.context = context;
        this.manager = manager;
        this.host = host;
        this.port = port;
        this.executor = executor;

        rec = new WifiDirectBroadcastReceiver(manager, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
                Toast.makeText(WifiP2PConnection.this.context, "Connected to: " + wifiP2pInfo.groupOwnerAddress, Toast.LENGTH_SHORT).show();
                Toast.makeText(WifiP2PConnection.this.context, "Am i owner?: " + wifiP2pInfo.isGroupOwner, Toast.LENGTH_SHORT).show();
                TCPClientConnector connector = new TCPClientConnector(wifiP2pInfo.groupOwnerAddress, WifiP2PConnection.this.port, WifiP2PConnection.this.executor);
                connector.setListener(WifiP2PConnection.this);
                WifiP2PConnection.this.executor.submit(connector);
            }
        });
    }

    public void setListener(ConnectedListener listener) {
        this.listener = listener;
    }


    @Override
    public void start() {
        if(listener == null)
            throw new IllegalStateException("No listener set");

        rec.register(context);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = host;
        config.wps.setup =  WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        manager.connect(config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int i) {
                LogHelper.e(TAG, "Can't connect: ", i);
            }
        });
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void onConnection(MessageConnection connection) {
        listener.OnConnected(connection);
    }

    public interface ConnectedListener {
        void OnConnected(MessageConnection connection);
    }
}
