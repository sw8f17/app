package rocks.stalin.android.app.network;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/13/17.
 */

public class WifiP2pServiceAnnouncer implements Runnable, Lifecycle {
    private static final String TAG = LogHelper.makeLogTag(WifiP2pServiceAnnouncer.class);

    private final WifiP2pDnsSdServiceInfo serviceInfo;
    private WifiP2PManagerFacade manager;

    private final TaskExecutor executor;

    private Semaphore removedLock = new Semaphore(0, true);

    private boolean running = false;
    private boolean initialized = false;

    public WifiP2pServiceAnnouncer(WifiP2PManagerFacade manager, int port, TaskExecutor executor) {
        this.manager = manager;

        this.executor = executor;

        Map<String, String> record = new HashMap<>();
        record.put("name", Build.MODEL);
        record.put("port", String.valueOf(port));
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(UUID.randomUUID().toString(), "_rtsp._tcp", record);
    }

    private void completeCall() {
        removedLock.release();
        if(removedLock.availablePermits() >= 32) {
            removedLock.acquireUninterruptibly(32);
            executor.submit(this);
        }
    }

    @Override
    public void start() {
        manager.removeGroup(null);

        removedLock.drainPermits();
        try {
            Method method = WifiP2pManager.class.getDeclaredMethod("deletePersistentGroup", WifiP2pManager.Channel.class, int.class, WifiP2pManager.ActionListener.class);
            for (int netid = 0; netid < 32; netid++) {
                try {
                    method.invoke(manager.getManager(), manager.getChannel(), netid, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            LogHelper.i(TAG, "Removed group");
                            completeCall();
                        }

                        @Override
                        public void onFailure(int reason) {
                            LogHelper.i(TAG, "Failure while removing group");
                            completeCall();
                        }
                    });
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LogHelper.w(TAG, "Error trying to call hidden method: deletePersistentGroup");
                    completeCall();
                }
            }
        } catch (NoSuchMethodException e) {
            LogHelper.w(TAG, "No method called deletePersistentGroup");
        }
        initialized = true;
    }

    @Override
    public void run() {
        running = true;
        if(!initialized)
            throw new IllegalStateException("Run called before initialize");

        manager.discoverPeers(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.createGroup(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        manager.addLocalService(serviceInfo, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                LogHelper.i(TAG, "Service added");
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
    }

    @Override
    public void stop() {
        running = false;
        manager.stopPeerDiscovery(null);
        manager.removeGroup(null);
        manager.removeLocalService(serviceInfo, null);
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
