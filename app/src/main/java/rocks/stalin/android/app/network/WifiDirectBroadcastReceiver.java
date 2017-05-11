package rocks.stalin.android.app.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import rocks.stalin.android.app.utils.LogHelper;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private WifiP2pManager.ConnectionInfoListener listener;

    public WifiDirectBroadcastReceiver(
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            WifiP2pManager.ConnectionInfoListener listener) {
        this.manager = manager;
        this.channel = channel;
        this.listener = listener;
    }

    public void register(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        context.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
            manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                    for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                        LogHelper.e("F", "FOUND ONE: ", device.deviceName);
                    }
                }
            });
        }
        if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)){
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {
                        context.unregisterReceiver(WifiDirectBroadcastReceiver.this);
                        listener.onConnectionInfoAvailable(info);
                    }
                });
            }
        }
    }
}
