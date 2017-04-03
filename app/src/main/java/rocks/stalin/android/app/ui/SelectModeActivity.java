package rocks.stalin.android.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import rocks.stalin.android.app.R;

/**
 * Created by delusional on 4/3/17.
 */

public class SelectModeActivity extends AppCompatActivity {
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
    }
}
