/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package rocks.stalin.android.app.utils;

import android.app.Activity;

import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;

/**
 * Generic reusable network methods.
 */
public class PermissionHelper {
    /**
     * @param context to use to check for network connectivity.
     * @return true if connected, false otherwise.
     */
    public static ArrayList<String> missingPermissions;
    public static final int SHOULD_RECREATE_ACTIVITY = 1;

    public static void requestMissingPermissions(Activity activity, int permission_req) {
        if(missingPermissions != null) {
            ActivityCompat.requestPermissions(activity, missingPermissions.toArray(new String[missingPermissions.size()]), permission_req);
        }
    }

    public static void addMissingPermission(String missing_permission) {
        if (missingPermissions == null) {
            missingPermissions = new ArrayList<>();
        }
        missingPermissions.add(missing_permission);
    }

    public static void removeMissingPermission(final String missing_permission) {
        for(String p:missingPermissions) {
            if(p.equals(missing_permission)){
                missingPermissions.remove(p);
            }
        }
    }
}
