package rocks.stalin.android.app.network;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Build;
import android.os.Looper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private Semaphore lock = new Semaphore(0, true);

    public WifiP2PMessageServer(WifiP2pManager p2pManager) {
        this.p2pManager = p2pManager;

        Map<String, String> record = new HashMap<>();
        record.put("name", Build.MODEL);
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(UUID.randomUUID().toString(), "_rtsp._tcp", record);
    }

    public void initialize(Context context, final InitializedListener listener) {
        channel = p2pManager.initialize(context, Looper.getMainLooper(), null);

        p2pManager.removeGroup(channel, null);

        lock.drainPermits();
        try {
            Method method = WifiP2pManager.class.getDeclaredMethod("deletePersistentGroup", WifiP2pManager.Channel.class, int.class, WifiP2pManager.ActionListener.class);
            for (int netid = 0; netid < 32; netid++) {
                try {
                    method.invoke(p2pManager, channel, netid, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            LogHelper.i(TAG, "Removed group");
                            lock.release();
                            if(lock.availablePermits() >= 32) {
                                try {
                                    lock.acquire(32);
                                    listener.onInitialized();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }

                        @Override
                        public void onFailure(int reason) {
                            LogHelper.i(TAG, "Failure while removing group");
                            lock.release();
                            if(lock.availablePermits() >= 32) {
                                try {
                                    lock.acquire(32);
                                    listener.onInitialized();
                                } catch (InterruptedException _) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    });
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LogHelper.w(TAG, "Error trying to call hidden method: deletePersistentGroup");
                    lock.release();
                    if(lock.availablePermits() >= 32) {
                        try {
                            lock.acquire(32);
                            listener.onInitialized();
                        } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            LogHelper.w(TAG, "No method called deletePersistentGroup");
        }
    }

    public void start(final ClientListener listener) {
        socketManager = new ServerListenerManager();
        try {
            socketManager.start(8009, new ServerListenerManager.ClientListener() {
                @Override
                public void connected(MessageConnection connection) {
                    listener.onNewClient(connection);
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
        p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                p2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        p2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                LogHelper.e(TAG, "Service added");
                            }

                            @Override
                            public void onFailure(int i) {
                                LogHelper.e(TAG, "Local service failed: ", i);
                            }
                        });
                    }

                    @Override
                    public void onFailure(int i) {
                        LogHelper.e(TAG, "Failed creating group: ", i);
                    }
                });
            }

            @Override
            public void onFailure(int i) {
                LogHelper.e(TAG, "Failed starting discovery: ", i);
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
    }

    public interface InitializedListener {
        void onInitialized();
    }
}
