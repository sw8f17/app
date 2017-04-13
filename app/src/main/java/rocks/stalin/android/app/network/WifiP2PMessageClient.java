package rocks.stalin.android.app.network;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rocks.stalin.android.app.ClientNetworkService;
import rocks.stalin.android.app.model.Group;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/13/17.
 */

public class WifiP2PMessageClient {
    private static final String TAG = LogHelper.makeLogTag(WifiP2PMessageClient.class);

    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;

    private WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance();

    private HashMap<String, Group> unfinished = new HashMap<>();

    public WifiP2PMessageClient(WifiP2pManager p2pManager) {
        this.p2pManager = p2pManager;
    }

    public void initialize(Context context) {
        channel = p2pManager.initialize(context, Looper.getMainLooper(), null);
    }

    public void discoverServers(final DiscoverListener listener) {
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

        p2pManager.setDnsSdResponseListeners(channel, servListener, txtListener);
        p2pManager.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                LogHelper.v(TAG, "Added service request for music service");
                p2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
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
    }

    public void stopDiscovery() {
        p2pManager.removeServiceRequest(channel, request, null);
    }

    public void connect(final Context context, String host, final int port) {
        //We don't need to discover when we are connected
        stopDiscovery();

        WifiDirectBroadcastReceiver rec =
                new WifiDirectBroadcastReceiver(p2pManager, channel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
                        Toast.makeText(context, "Connected to: " + wifiP2pInfo.groupOwnerAddress, Toast.LENGTH_SHORT).show();
                        Toast.makeText(context, "Am i owner?: " + wifiP2pInfo.isGroupOwner, Toast.LENGTH_SHORT).show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Socket socket = new Socket(wifiP2pInfo.groupOwnerAddress, port);
                                    MessageConnection connection = new MessageConnection(socket);
                                    connection.start();
                                    connection.addHandler(Welcome.class, new MessageConnection.MessageListener<Welcome, Welcome.Builder>() {
                                        @Override
                                        public void packetReceived(Welcome message) {
                                            LogHelper.e(TAG, "DATA: ", message.song_name);
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        context.registerReceiver(rec, intentFilter);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = host;
        config.wps.setup =  WpsInfo.PBC;
        p2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int i) {
                LogHelper.e(TAG, "Can't connect: ", i);
            }
        });
    }

    public void disconnect() {
        //TODO: Stub
    }

    public interface DiscoverListener {
        void onServerDiscovered(Group group);
    }
}
