package rocks.stalin.android.app.network;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.ObservableFuture;
import rocks.stalin.android.app.framework.concurrent.SettableFutureImpl;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.functional.Consumer;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/13/17.
 */

public class WifiP2PConnectionFactory {
    private static final String TAG = LogHelper.makeLogTag(WifiP2PConnectionFactory.class);

    private Context context;
    private final WifiP2PManagerFacade manager;
    private TaskExecutor executor;


    public WifiP2PConnectionFactory(Context context, WifiP2PManagerFacade manager, TaskExecutor executor) {
        this.context = context;
        this.manager = manager;
        this.executor = executor;

    }

    public ObservableFuture<MessageConnection> connect(String host, final int port) {
        final SettableFutureImpl<MessageConnection> future = new SettableFutureImpl<>();

        final WifiDirectBroadcastReceiver rec = new WifiDirectBroadcastReceiver(manager, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
                Toast.makeText(WifiP2PConnectionFactory.this.context, "Connected to: " + wifiP2pInfo.groupOwnerAddress, Toast.LENGTH_SHORT).show();
                Toast.makeText(WifiP2PConnectionFactory.this.context, "Am i owner?: " + wifiP2pInfo.isGroupOwner, Toast.LENGTH_SHORT).show();
                TCPClientConnector connector = new TCPClientConnector(wifiP2pInfo.groupOwnerAddress, port, executor);
                future.setFuture(connector.create());
            }
        });

        rec.register(context);

        final WifiP2pConfig config = new WifiP2pConfig();
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
                rec.unregister(context);
            }
        });
        return future;
    }
}
