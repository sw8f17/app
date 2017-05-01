package rocks.stalin.android.app.ui;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import rocks.stalin.android.app.ClientMusicService;
import rocks.stalin.android.app.R;
import rocks.stalin.android.app.model.Group;
import rocks.stalin.android.app.network.WifiP2PMessageClient;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/3/17.
 */

public class SelectGroupActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(SelectGroupActivity.class);

    RecyclerView rv;

    List<Group> groupList = new ArrayList<>();
    GroupItemAdapter adapter;

    WifiP2PMessageClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group);

        WifiP2pManager manager = getSystemService(WifiP2pManager.class);
        client = new WifiP2PMessageClient(manager);
        client.initialize(this);

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
                //ClientConnectedActivity.start(SelectGroupActivity.this, group.address);
                FullScreenClientActivity.start(SelectGroupActivity.this, group.address);
            }
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        client.discoverServers(new WifiP2PMessageClient.DiscoverListener() {
            @Override
            public void onServerDiscovered(Group group) {
                if(!groupList.contains(group)) {
                    groupList.add(group);
                    adapter.notifyItemInserted(groupList.size() - 1);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        client.stopDiscovery();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        client.stopDiscovery();
    }
}
