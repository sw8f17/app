package rocks.stalin.android.app.network;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Build;
import android.os.Looper;

import com.squareup.wire.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rocks.stalin.android.app.proto.SongMetaData;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.utils.LogHelper;

import static android.content.ContentValues.TAG;

/**
 * Created by delusional on 4/13/17.
 */

public class WifiP2PMessageServer {
    private final WifiP2pDnsSdServiceInfo serviceInfo;
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private ServerListenerManager socketManager;

    public WifiP2PMessageServer(WifiP2pManager p2pManager) {
        this.p2pManager = p2pManager;

        Map<String, String> record = new HashMap<>();
        record.put("name", Build.MODEL);
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(UUID.randomUUID().toString(), "_rtsp._tcp", record);
    }

    public void initialize(Context context) {
        channel = p2pManager.initialize(context, Looper.getMainLooper(), null);

        p2pManager.removeGroup(channel, null);
    }

    public void start(final ClientListener listener) {
        socketManager = new ServerListenerManager();
        try {
            socketManager.start(8009, new ServerListenerManager.ClientListener() {
                @Override
                public void connected(MessageConnection connection) {
                    listener.onNewClient(connection);
                }

                @Override
                public <M extends Message<M, B>, B extends Message.Builder<M, B>> void sendProto(MessageConnection connection, M packet, Class<M> clazz) {
                    listener.sendProto(connection, packet, clazz);
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
                                        p2pManager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                                            @Override
                                            public void onSuccess() {
                                                p2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
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

    public void stop() throws IOException {
        socketManager.stop();
        p2pManager.stopPeerDiscovery(channel, null);
        p2pManager.removeGroup(channel, null);
        p2pManager.removeLocalService(channel, serviceInfo, null);
    }

    public interface ClientListener {
        void onNewClient(MessageConnection connection);

        <M extends Message<M, B>, B extends Message.Builder<M, B>> void sendProto(MessageConnection connection, M message, Class<M> clazz);
    }
}
