package rocks.stalin.android.app.network;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rocks.stalin.android.app.ClientNetworkService;
import rocks.stalin.android.app.decoding.MP3Encoding;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.model.Group;
import rocks.stalin.android.app.playback.LocalAudioMixer;
import rocks.stalin.android.app.playback.LocalSoundSink;
import rocks.stalin.android.app.playback.actions.PlayAction;
import rocks.stalin.android.app.proto.Music;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

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

        try {
            Method method = WifiP2pManager.class.getDeclaredMethod("deletePersistentGroup", WifiP2pManager.Channel.class, int.class, WifiP2pManager.ActionListener.class);
            for (int netid = 0; netid < 32; netid++) {
                try {
                    method.invoke(p2pManager, channel, netid, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            LogHelper.i(TAG, "Removed group");
                        }

                        @Override
                        public void onFailure(int reason) {
                            LogHelper.i(TAG, "Failure while removing group");
                        }
                    });
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LogHelper.w(TAG, "Error trying to call hidden method: deletePersistentGroup");
                }
            }
        } catch (NoSuchMethodException e) {
            LogHelper.w(TAG, "No method called deletePersistentGroup");
        }
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
        p2pManager.removeGroup(channel, null);
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

        final LocalAudioMixer localAudioMixer = new LocalAudioMixer(new MP3MediaInfo(44100, 1, 0, MP3Encoding.UNSIGNED16), Clock.getTime());
        LocalSoundSink sink = new LocalSoundSink(localAudioMixer);
        sink.change(new MP3MediaInfo(44100, 1, 0, MP3Encoding.UNSIGNED16));

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
                                    Socket socket = new Socket();
                                    socket.setSoTimeout(0);
                                    socket.connect(new InetSocketAddress(wifiP2pInfo.groupOwnerAddress, port));
                                    MessageConnection connection = new MessageConnection(socket);
                                    connection.start();
                                    connection.addHandler(Welcome.class, new MessageConnection.MessageListener<Welcome, Welcome.Builder>() {
                                        @Override
                                        public void packetReceived(Welcome message) {
                                            LogHelper.e(TAG, "DATA: ", message.song_name);
                                        }
                                    });
                                    connection.addHandler(PlayCommand.class, new MessageConnection.MessageListener<PlayCommand, PlayCommand.Builder>() {
                                        @Override
                                        public void packetReceived(PlayCommand message) {
                                            LogHelper.e(TAG, "PLAY: ", message.playtime.millis);
                                            Clock.Instant time = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                                            PlayAction action = new PlayAction(time);
                                            localAudioMixer.pushAction(action);
                                        }
                                    });
                                    connection.addHandler(Music.class, new MessageConnection.MessageListener<Music, Music.Builder>() {
                                        @Override
                                        public void packetReceived(Music message) {
                                            LogHelper.e(TAG, "MUSIC: ", message.playtime.millis);
                                            Clock.Instant playTime = new Clock.Instant(message.playtime.millis, message.playtime.nanos);
                                            localAudioMixer.pushFrame(playTime, message.data.asByteBuffer());
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
        config.groupOwnerIntent = 0;
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
