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
import rocks.stalin.android.app.network.WifiP2PManagerFacade;
import rocks.stalin.android.app.network.WifiP2PMarcoPolo;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/3/17.
 */

public class SelectGroupActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(SelectGroupActivity.class);

    RecyclerView rv;

    List<Group> groupList = new ArrayList<>();
    GroupItemAdapter adapter;

    WifiP2PMarcoPolo discoverer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group);

        WifiP2pManager rawManager = getSystemService(WifiP2pManager.class);
        WifiP2pManager.Channel channel = rawManager.initialize(this, getMainLooper(), null);
        WifiP2PManagerFacade manager = new WifiP2PManagerFacade(rawManager, channel);

        discoverer = new WifiP2PMarcoPolo(manager);
        discoverer.setListener(new WifiP2PMarcoPolo.DiscoverListener() {
            @Override
            public void onServerDiscovered(Group group) {
                groupList.add(group);
                adapter.notifyItemInserted(groupList.size()-1);
            }
        });

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
                discoverer.stop();
            }
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        discoverer.start();
    }

    @Override
    protected void onPause() {
        discoverer.stop();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        discoverer.stop();
        Intent i = new Intent(this, ClientMusicService.class);
        stopService(i);
    }
}
