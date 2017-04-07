package rocks.stalin.android.app.ui;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.stalin.android.app.ClientMusicService;
import rocks.stalin.android.app.NetworkService;
import rocks.stalin.android.app.R;
import rocks.stalin.android.app.model.Group;
import rocks.stalin.android.app.network.WifiDirectBroadcastReceiver;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/3/17.
 */

public class SelectGroupActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(SelectGroupActivity.class);

    RecyclerView rv;

    List<Group> groupList = new ArrayList<>();
    Map<String, Group> groups = new HashMap<>();
    GroupItemAdapter adapter;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance();

    WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
        @Override
        public void onDnsSdTxtRecordAvailable(String s, Map<String, String> map, WifiP2pDevice wifiP2pDevice) {
            Group group = new Group(s, wifiP2pDevice.deviceAddress, map.get("name"));
            groupList.add(group);
            groups.put(wifiP2pDevice.deviceAddress, group);
            adapter.notifyItemInserted(groupList.size()-1);
        }
    };

    WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
        @Override
        public void onDnsSdServiceAvailable(String s, String s1, WifiP2pDevice wifiP2pDevice) {
            Group group = groups.get(wifiP2pDevice.deviceAddress);
            group.id = s;
            adapter.notifyItemChanged(groupList.indexOf(group));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group);

        manager = getSystemService(WifiP2pManager.class);
        channel = manager.initialize(this, getMainLooper(), null);

        rv = (RecyclerView) findViewById(R.id.rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rv.getContext(),
                layoutManager.getOrientation());
        rv.addItemDecoration(dividerItemDecoration);
        adapter = new GroupItemAdapter(groupList, new GroupItemAdapter.ItemSelectedListener() {
            @Override
            public void select(Group group) {
                Toast.makeText(SelectGroupActivity.this, "ID: " + group.name, Toast.LENGTH_SHORT).show();
                Intent i = new Intent(SelectGroupActivity.this, ClientMusicService.class);
                i.setAction(ClientMusicService.ACTION_CONNECT);
                i.putExtra(ClientMusicService.CONNECT_HOST_NAME, group.address);
                i.putExtra(ClientMusicService.CONNECT_PORT_NAME, 8009);
                startService(i);
            }
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        manager.setDnsSdResponseListeners(channel, servListener, txtListener);
        manager.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                LogHelper.e(TAG, "Woo");
                manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        LogHelper.e(TAG, "Woo2");
                    }

                    @Override
                    public void onFailure(int i) {
                        LogHelper.e(TAG, "Noo2");
                    }
                });
            }

            @Override
            public void onFailure(int i) {
                LogHelper.e(TAG, "Noo");
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        manager.removeServiceRequest(channel, request, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int i) {
            }
        });
        Intent i = new Intent(this, ClientMusicService.class);
        stopService(i);
    }
}
