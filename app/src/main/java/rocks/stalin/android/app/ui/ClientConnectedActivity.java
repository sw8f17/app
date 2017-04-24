package rocks.stalin.android.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import rocks.stalin.android.app.ClientMusicService;
import rocks.stalin.android.app.R;
import rocks.stalin.android.app.utils.LogHelper;

public class ClientConnectedActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(ClientConnectedActivity.class);
    private static final String EXTRA_OWNER_ADDRESS = "rocks.stalin.android.app.OWNER_ADDRESS";

    public static void start(Context context, String ownerAddress) {
        Intent intent = new Intent(context, ClientConnectedActivity.class);
        intent.putExtra(EXTRA_OWNER_ADDRESS, ownerAddress);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_connected);

        Intent intent = this.getIntent();
        String ownerAddress = intent.getStringExtra(EXTRA_OWNER_ADDRESS);
        Intent i = new Intent(ClientConnectedActivity.this, ClientMusicService.class);
        i.setAction(ClientMusicService.ACTION_CONNECT);
        i.putExtra(ClientMusicService.CONNECT_HOST_NAME, ownerAddress);
        i.putExtra(ClientMusicService.CONNECT_PORT_NAME, 8009);
        LogHelper.i(TAG, "Starting client music service");
        startService(i);
        TextView tv = (TextView) findViewById(R.id.client_connected_textview);
        tv.setText("Owner MAC addr: " + ownerAddress);

        Button b = (Button) findViewById(R.id.disconnect);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogHelper.i(TAG, "Disconnect button pressed");
                Toast.makeText(ClientConnectedActivity.this, "Disconnecting", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent i = new Intent(this, ClientMusicService.class);
        stopService(i);
    }
}
