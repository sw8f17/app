package rocks.stalin.android.app.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;

import rocks.stalin.android.app.ClientMusicService;
import rocks.stalin.android.app.MusicService;
import rocks.stalin.android.app.utils.LogHelper;

public class FullScreenClientActivity extends FullScreenActivity {
    private static final String TAG = LogHelper.makeLogTag(FullScreenClientActivity.class);
    private static final String EXTRA_OWNER_ADDRESS = "rocks.stalin.android.app.OWNER_ADDRESS";

    public static void start(Context context, String ownerAddress) {
        Intent intent = new Intent(context, FullScreenClientActivity.class);
        intent.putExtra(EXTRA_OWNER_ADDRESS, ownerAddress);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        Intent intent = this.getIntent();
        String ownerAddress = intent.getStringExtra(EXTRA_OWNER_ADDRESS);
        Intent i = new Intent(this, ClientMusicService.class);
        i.setAction(ClientMusicService.ACTION_CONNECT);
        i.putExtra(ClientMusicService.CONNECT_HOST_NAME, ownerAddress);
        i.putExtra(ClientMusicService.CONNECT_PORT_NAME, 8009);
        LogHelper.i(TAG, "Starting client music service");
        startService(i);

        mLine1.setText("Hello!");

        LogHelper.i(TAG, "Finished creating the fullscreen client");
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
