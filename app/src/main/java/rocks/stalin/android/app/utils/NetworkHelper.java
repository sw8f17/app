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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutionException;

import rocks.stalin.android.app.network.SntpOffsetTask;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Generic reusable network methods.
 */
public class NetworkHelper {
    /**
     * @param context to use to check for network connectivity.
     * @return true if connected, false otherwise.
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static Clock.Duration offset;
    static {
        offset = getOffset();
    }

    @Nullable
    public static Clock.Duration getOffset() {
        Long offset = clockOffset();
        return offset == null ? null : Clock.Duration.fromMillis(offset);
    }

    @Nullable
    public static Long clockOffset(){
        SntpOffsetTask task = new SntpOffsetTask();
        task.execute();
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
}
