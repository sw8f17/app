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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.Iterator;

import rocks.stalin.android.app.utils.LogHelper;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class ExternalStorageSource implements MusicProviderSource {

    private static final String TAG = LogHelper.makeLogTag(ExternalStorageSource.class);
    private ContentResolver mContentResolver;

    public ExternalStorageSource(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        try {
            Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            Cursor musicCursor = mContentResolver.query(musicUri, null, null, null, null);
            ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
            if (musicCursor != null && musicCursor.moveToFirst()) {
                int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

                do {
                    LogHelper.d(TAG, "i'm in da do");
                    tracks.add(new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicCursor.getString(idColumn))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicCursor.getString(titleColumn))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicCursor.getString(artistColumn))
                            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Dank")
                            .build());
                } while (musicCursor.moveToNext());
                musicCursor.close();
            }
            return tracks.iterator();
        } catch (Exception e) {
            LogHelper.e(TAG, e, "Could not retrieve music list");
            throw new RuntimeException("Could not retrieve music list", e);
        }
    }
}
