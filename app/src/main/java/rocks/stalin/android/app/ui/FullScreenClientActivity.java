package rocks.stalin.android.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.widget.Toast;

import rocks.stalin.android.app.ClientMusicService;
import rocks.stalin.android.app.utils.LogHelper;

public class FullScreenClientActivity extends FullScreenActivity {
    private static final String TAG = LogHelper.makeLogTag(FullScreenClientActivity.class);
    private static final String EXTRA_OWNER_ADDRESS = "rocks.stalin.android.app.OWNER_ADDRESS";
    public static final String BROADCAST_ACTION_IDENTIFIER = "rocks.stalin.android.app.SONG_METADATA";

    Context context;
    BroadcastReceiver metadataReciver;
    IntentFilter filter;

    public static void start(Context context, String ownerAddress) {
        Intent intent = new Intent(context, FullScreenClientActivity.class);
        intent.putExtra(EXTRA_OWNER_ADDRESS, ownerAddress);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filter = new IntentFilter();
        filter.addAction(BROADCAST_ACTION_IDENTIFIER);
        metadataReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null) {
                    // DO STUFF
                    LogHelper.e(TAG, "BROADCAST RECEIVED");
                    String songName = intent.getStringExtra("SONG_NAME");
                    String artistName = intent.getStringExtra("ARTIST_NAME");
                    String backgroundUrl = intent.getStringExtra("BACKGROUND_IMAGE_URL");
                    mLine1.setText(songName);
                    mLine2.setText(artistName);
                    fetchImageAsync(backgroundUrl);
                }
            }
        };
        registerReceiver(metadataReciver, filter);

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
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(metadataReciver);
    }
}
