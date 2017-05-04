package rocks.stalin.android.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import rocks.stalin.android.app.R;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/3/17.
 */

public class SelectModeActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(SelectModeActivity.class);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);

        findViewById(R.id.select_mode_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SelectModeActivity.this, MusicPlayerActivity.class);
                startActivity(i);
            }
        });

        findViewById(R.id.select_mode_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SelectModeActivity.this, SelectGroupActivity.class);
                startActivity(i);
            }
        });

        /*
        MP3Decoder decoder = new MP3Decoder();
        decoder.init();
        long handle = decoder.open("/sdcard/Download/bensound-dubstep.mp3");

        byte[] data;
        AudioTrack t = new AudioTrack.Builder()
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(73728*20)
                .build();
        do {
            data = decoder.decodeFrame(handle);
            LogHelper.e(TAG, "Data length: ", data.length);
            LogHelper.e(TAG, "Written ", t.write(data, 0, data.length), " to audiotrack");
            t.play();
            t.flush();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }while(data.length != 0);
        decoder.close(handle);
        decoder.exit();
        */
    }
}
