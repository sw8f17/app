package rocks.stalin.android.app.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by Mathias on 13-Mar-17.
 */

public class PermissionHelper {

    public static final int THROW_IF_DENIED = 0;

    private Activity mActivity;

    public PermissionHelper(Activity activity) {
        this.mActivity = activity;
    }

    public void getPermissionOrThrow(String permission) {
        if (ContextCompat.checkSelfPermission(mActivity,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                    permission)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(mActivity,
                        new String[]{permission},
                        THROW_IF_DENIED);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    }
}
