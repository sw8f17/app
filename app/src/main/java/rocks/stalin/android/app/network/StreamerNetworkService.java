package rocks.stalin.android.app.network;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Build;
import android.widget.Toast;

import com.squareup.wire.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;

import static android.os.Looper.getMainLooper;

/**
 * Created by delusional on 4/5/17.
 */

public class StreamerNetworkService {
    private static final String TAG = LogHelper.makeLogTag(StreamerNetworkService.class);

    public WifiP2pManager p2pManager;
    public WifiP2pManager.Channel channel;
    private final WifiP2pDnsSdServiceInfo test;
    ServerListenerManager socketManager;

    public StreamerNetworkService(WifiP2pManager p2pManager) {
        this.p2pManager = p2pManager;

        Map<String, String> record = new HashMap<>();
        record.put("name", Build.MODEL);
        test = WifiP2pDnsSdServiceInfo.newInstance(UUID.randomUUID().toString(), "_rtsp._tcp", record);
    }

    public void initialize(Context srcContext) {
        //TODO: WAHT THE FUCK IS A CHANNEL LISTENER
        channel = p2pManager.initialize(srcContext, getMainLooper(), null);

        p2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int i) {
            }
        });

    }

    public void listen() {
        socketManager = new ServerListenerManager();
        try {
            socketManager.start(8009, new ServerListenerManager.ClientListener() {
                @Override
                public void connected(Socket socket) {
                    Welcome packet = new Welcome.Builder()
                            .song_name("Darude - Sandstorm")
                            .build();
                    try {
                        byte[] packetData = Welcome.ADAPTER.encode(packet);
                        OutputStream stream = socket.getOutputStream();
                        stream.write(packetData);
                        stream.flush();
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: _presence.who?
        //This is some IANA standardized shit
        //HACK: I don't know why this works, but it seems like it's required for the service to be
        //Discoverable. I think this might time out after 5 mins, so we'll need to run this in a
        //loop or something... WTF
        LogHelper.i(TAG, "CALLBACK HELL INITIATED");
        p2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                p2pManager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                p2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        p2pManager.removeLocalService(channel, test, new WifiP2pManager.ActionListener() {
                                            @Override
                                            public void onSuccess() {
                                                p2pManager.addLocalService(channel, test, new WifiP2pManager.ActionListener() {
                                                    @Override
                                                    public void onSuccess() {
                                                        LogHelper.e(TAG, "Service added");
                                                    }

                                                    @Override
                                                    public void onFailure(int i) {
                                                        LogHelper.e(TAG, "Local service failed");
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(int i) {
                                                LogHelper.e(TAG, "Failed removing local service");
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(int i) {
                                        LogHelper.e(TAG, "Failed creating group");
                                    }
                                });
                            }

                            @Override
                            public void onFailure(int i) {
                                LogHelper.e(TAG, "Failed discovering peers");
                            }
                        });
                    }

                    @Override
                    public void onFailure(int i) {
                        LogHelper.e(TAG, "Failed clearing services");
                    }
                });
            }

            @Override
            public void onFailure(int i) {
            }
        });
        LogHelper.i(TAG, "STAY FROSTY");
    }

    public void stopListen() throws IOException {
        socketManager.stop();
        p2pManager.stopPeerDiscovery(channel, null);
        p2pManager.removeGroup(channel, null);
        p2pManager.removeLocalService(channel, test, null);
    }

    public void connect(final Context context, String host, final int port) {
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
                                    Welcome packet = Welcome.ADAPTER.decode(socket.getInputStream());
                                    LogHelper.e(TAG, "DATA: ", packet.song_name);
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
}
