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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Group group = (Group) o;

        return id.equals(group.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
