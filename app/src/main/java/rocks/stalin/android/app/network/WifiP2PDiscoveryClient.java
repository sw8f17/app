package rocks.stalin.android.app.network;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

import java.util.HashMap;
import java.util.Map;

import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.model.Group;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 5/12/17.
 */

public class WifiP2PDiscoveryClient implements Lifecycle {
    private static final String TAG = LogHelper.makeLogTag(WifiP2PDiscoveryClient.class);

    private final WifiP2PManagerFacade manager;

    private DiscoverListener listener;

    private WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance();
    private HashMap<String, Group> unfinished = new HashMap<>();

    private boolean running = false;

    public WifiP2PDiscoveryClient(WifiP2PManagerFacade manager) {
        this.manager = manager;
    }

    public void setListener(DiscoverListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        if(listener == null)
            throw new IllegalStateException("No listener set");

        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String s, Map<String, String> map, WifiP2pDevice wifiP2pDevice) {
                Group group = new Group(s, wifiP2pDevice.deviceAddress, map.get("name"));
                unfinished.put(wifiP2pDevice.deviceAddress, group);
            }
        };
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String s, String s1, WifiP2pDevice wifiP2pDevice) {
                Group group = unfinished.remove(wifiP2pDevice.deviceAddress);
                group.id = s;
                listener.onServerDiscovered(group);
            }
        };

        manager.setDnsSdResponseListeners(servListener, txtListener);
        manager.removeGroup(null);
        manager.addServiceRequest(request, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                LogHelper.v(TAG, "Added service request for music service");
                manager.discoverServices(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        LogHelper.i(TAG, "Started service discovery");
                    }

                    @Override
                    public void onFailure(int i) {
                        LogHelper.e(TAG, "Service discovery failed: ", i);
                    }
                });
            }

            @Override
            public void onFailure(int i) {
                LogHelper.e(TAG, "Adding service request failed: ", i);
            }
        });
        running = true;
    }

    @Override
    public void stop() {
        manager.removeServiceRequest(request, null);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public interface DiscoverListener {
        void onServerDiscovered(Group group);
    }
}
