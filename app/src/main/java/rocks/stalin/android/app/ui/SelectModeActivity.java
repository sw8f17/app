package rocks.stalin.android.app.ui;

import android.content.Intent;
import android.icu.util.TimeUnit;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.concurrent.ExecutionException;

import rocks.stalin.android.app.R;
import rocks.stalin.android.app.network.SntpOffsetTask;


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

        findViewById(R.id.select_mode_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SelectModeActivity.this, SelectGroupActivity.class);
                startActivity(i);
            }
        });
    }
}
