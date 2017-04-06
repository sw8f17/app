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

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;

/**
 * Helper to be used for all things related to permissions.
 * The activities used when requesting permissions must implement the
 * onRequestPermissionsResult() method
 */
public class PermissionHelper {

    private static ArrayList<String> missingPermissions;
    public static final int SHOULD_RECREATE_ACTIVITY = 1;

    /**
     * Request all permissions in the ArrayList missingPermissions in a given activity
     *
     * @param activity to be used for asking for permissions
     * @param permission_req code to be checked in activity after permisison granting
     */
    public static void requestMissingPermissions(@NonNull Activity activity, @NonNull int permission_req) {
        if(missingPermissions != null) {
            ActivityCompat.requestPermissions(activity, missingPermissions.toArray(new String[missingPermissions.size()]), permission_req);
        }
    }

    /**
     * Request a single permission in a given activity
     *
     * @param activity to be used for asking for permissions
     * @param missing_permission string representing the single permission to be requested
     * @param permission_req code to be checked in activity after permisison granting
     */
    public static void requestMissingPermission(@NonNull Activity activity, @NonNull String missing_permission, @NonNull int permission_req) {
        ActivityCompat.requestPermissions(activity, new String[]{missing_permission}, permission_req);
    }

    /**
     * Add a missing permission to the static list of missing permissions.
     *
     * @param missing_permission android string representing the permission
     */
    public static void addMissingPermission(@NonNull String missing_permission) {
        if (missingPermissions == null) {
            missingPermissions = new ArrayList<>();
        }
        missingPermissions.add(missing_permission);
    }

    /**
     * Remove all occurrences of a permission from the static list of missing permissions.
     *
     * @param missing_permission android string representing the permission
     */
    public static void removeMissingPermission(@NonNull String missing_permission) {
        for(String p:missingPermissions) {
            if(p.equals(missing_permission)){
                missingPermissions.remove(p);
            }
        }
    }

    public static ArrayList<String> getMissingPermissions() {
        if (missingPermissions == null) {
            missingPermissions = new ArrayList<>();
        }
        return missingPermissions;
    }

    public static void removeAllMissingPermissions() {
        missingPermissions.clear();
    }
}
