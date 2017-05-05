package rocks.stalin.android.app.utils.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.ACTION_TIME_CHANGED;

/**
 * Created by delusional on 5/4/17.
 */

public class ClockBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Clock.sync();
    }
}
