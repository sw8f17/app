package rocks.stalin.android.app.model;

import android.net.wifi.p2p.WifiP2pConfig;

/**
 * Created by delusional on 4/4/17.
 */

public class Group {
    public String id;
    public String name;
    public String address;

    public Group(String id, String address, String name) {
        this.id = id;
        this.address = address;
        this.name = name;
    }
}
