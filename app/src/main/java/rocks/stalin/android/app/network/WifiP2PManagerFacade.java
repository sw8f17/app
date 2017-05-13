package rocks.stalin.android.app.network;


import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

/**
 * Created by delusional on 5/13/17.
 */

public class WifiP2PManagerFacade {
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;

    public WifiP2PManagerFacade(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        this.manager = manager;
        this.channel = channel;
    }

    public WifiP2pManager getManager() {
        return manager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public void setDnsSdResponseListeners(WifiP2pManager.DnsSdServiceResponseListener servListener, WifiP2pManager.DnsSdTxtRecordListener txtListener) {
        manager.setDnsSdResponseListeners(channel, servListener, txtListener);
    }

    public void removeGroup(WifiP2pManager.ActionListener listener) {
        manager.removeGroup(channel, listener);
    }

    public void discoverServices(WifiP2pManager.ActionListener listener) {
        manager.discoverServices(channel, listener);
    }

    public void addServiceRequest(WifiP2pDnsSdServiceRequest request, WifiP2pManager.ActionListener listener) {
        manager.addServiceRequest(channel, request, listener);
    }

    public void removeServiceRequest(WifiP2pDnsSdServiceRequest request, WifiP2pManager.ActionListener listener) {
        manager.removeServiceRequest(channel, request, listener);
    }

    public void requestPeers(WifiP2pManager.PeerListListener f) {
        manager.requestPeers(channel, f);
    }

    public void requestConnectionInfo(WifiP2pManager.ConnectionInfoListener connectionInfoListener) {
        manager.requestConnectionInfo(channel, connectionInfoListener);
    }

    public void connect(WifiP2pConfig config, WifiP2pManager.ActionListener listener) {
        manager.connect(channel, config, listener);
    }

    public void stopPeerDiscovery(WifiP2pManager.ActionListener listener) {
        manager.stopPeerDiscovery(channel, listener);
    }

    public void removeLocalService(WifiP2pDnsSdServiceInfo serviceInfo, WifiP2pManager.ActionListener listener) {
        manager.removeLocalService(channel, serviceInfo, listener);
    }

    public void addLocalService(WifiP2pDnsSdServiceInfo serviceInfo, WifiP2pManager.ActionListener listener) {
        manager.addLocalService(channel, serviceInfo, listener);
    }

    public void createGroup(WifiP2pManager.ActionListener listener) {
        manager.createGroup(channel, listener);
    }

    public void discoverPeers(WifiP2pManager.ActionListener listener) {
        manager.discoverServices(channel, listener);
    }
}
