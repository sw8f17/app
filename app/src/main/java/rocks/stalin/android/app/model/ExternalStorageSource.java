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

package rocks.stalin.android.app.model;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.Iterator;

import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.PermissionHelper;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class ExternalStorageSource implements MusicProviderSource {

    private static final String TAG = LogHelper.makeLogTag(ExternalStorageSource.class);
    private ContentResolver mContentResolver;
    private Context mContext;

    public ExternalStorageSource(Context ctx) {
        mContentResolver = ctx.getContentResolver();
        mContext = ctx;
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                Cursor musicCursor = mContentResolver.query(musicUri, null, null, null, null);
                if (musicCursor != null && musicCursor.moveToFirst()) {
                    int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    int isMusicColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);

                    do {
                        LogHelper.d(TAG, "i'm in da do");
                        if (musicCursor.getInt(isMusicColumn) != 0) {
                            tracks.add(new MediaMetadataCompat.Builder()
                                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicCursor.getString(idColumn))
                                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicCursor.getString(titleColumn))
                                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicCursor.getString(artistColumn))
                                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicCursor.getString(albumColumn))
                                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, "https://raw.githubusercontent.com/sw8f17/logo/master/scotty.png")
                                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Unknown")
                                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicCursor.getLong(durationColumn))
                                    .build());
                        }
                    } while (musicCursor.moveToNext());
                    musicCursor.close();
                }
            } catch (Exception e) {
                LogHelper.e(TAG, e, "Could not retrieve music list");
                throw new RuntimeException("Could not retrieve music list", e);
            }
        } else {
            PermissionHelper.addMissingPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        return tracks.iterator();
    }
}
