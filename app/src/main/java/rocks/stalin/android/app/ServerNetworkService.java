package rocks.stalin.android.app;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;

import rocks.stalin.android.app.network.MessageConnection;
import rocks.stalin.android.app.network.WifiP2PMessageServer;
import rocks.stalin.android.app.proto.Welcome;
import rocks.stalin.android.app.proto.SongMetaData;

/**
 * Created by delusional on 4/5/17.
 */

public class ServerNetworkService extends Service {
    public static final String MODE_NAME = "MODE_NAME";
    public static final String MODE_SERVER = "MODE_SERVER";
    public static final String MODE_CLIENT = "MODE_CLIENT";

    public static final String CLIENT_HOST_NAME = "CLIENT_HOST_NAME";
    public static final String CLIENT_PORT_NAME = "CLIENT_PORT_NAME";

    private WifiP2PMessageServer server;

    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        WifiP2pManager manager = getSystemService(WifiP2pManager.class);
        server = new WifiP2PMessageServer(manager);
        server.initialize(this);
    }

    public void startServer() {
        server.start(new WifiP2PMessageServer.ClientListener() {
            @Override
            public void onNewClient(MessageConnection connection) {
                Welcome packet = new Welcome.Builder()
                        .song_name("Darude - Sandstorm")
                        .build();
                Welcome packet2 = new Welcome.Builder()
                        .song_name("Darude - Dankstorm")
                        .build();
                SongMetaData songMetaData = new SongMetaData.Builder()
                        .song_name("Sandstorm")
                        .artist_name("Darude")
                        .background_image_url("http://i3.kym-cdn.com/photos/images/facebook/000/862/065/0e9.jpg")
                        .build();
                try {
                    connection.send(packet, Welcome.class);
                    connection.send(packet2, Welcome.class);
                    connection.send(songMetaData, SongMetaData.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public class LocalBinder extends Binder {
        ServerNetworkService getService() {
            return ServerNetworkService.this;
        }
    }
}
