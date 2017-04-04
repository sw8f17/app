package rocks.stalin.android.app.ui;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rocks.stalin.android.app.R;
import rocks.stalin.android.app.model.Group;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/3/17.
 */

public class SelectGroupActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(SelectGroupActivity.class);

    WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
        @Override
        public void onDnsSdTxtRecordAvailable(String s, Map<String, String> map, WifiP2pDevice wifiP2pDevice) {
            LogHelper.e(TAG, "Got em: ", s);
        }
    };

    WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
        @Override
        public void onDnsSdServiceAvailable(String s, String s1, WifiP2pDevice wifiP2pDevice) {
            LogHelper.e(TAG, "Got em2: ", s);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group);

        WifiP2pManager manager = getSystemService(WifiP2pManager.class);
        WifiP2pManager.Channel channel = manager.initialize(this, getMainLooper(), null);

        manager.setDnsSdResponseListeners(channel, servListener, txtListener);
        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                LogHelper.e(TAG, "Woo");
            }

            @Override
            public void onFailure(int i) {
                LogHelper.e(TAG, "Noo");
            }
        });

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

        List<Group> groups = new ArrayList<Group>();
        groups.add(new Group("Test1"));
        groups.add(new Group("Test2"));

        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rv.getContext(),
                layoutManager.getOrientation());
        rv.addItemDecoration(dividerItemDecoration);
        GroupItemAdapter adapter = new GroupItemAdapter(groups);
        rv.setAdapter(adapter);
    }
}
