package rocks.stalin.android.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.MP3File;

/**
 * Created by delusional on 4/10/17.
 */

public class MP3Decoder {
    private static final String TAG = LogHelper.makeLogTag(MP3Decoder.class);

    private static native void staticInit();

    public native void init();
    public native void exit();

    private native MP3File openFromDataSource(int fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException;

    static {
        System.loadLibrary("native-lib");
        staticInit();
    }

    public MP3File open(Context context, Uri uri) throws IOException, IllegalArgumentException,
            SecurityException, IllegalStateException {
        final ContentResolver resolver = context.getContentResolver();
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            return open(uri.getPath());
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                && Settings.AUTHORITY.equals(uri.getAuthority())) {
            // Try cached ringtone first since the actual provider may not be
            // encryption aware, or it may be stored on CE media storage
            final int type = RingtoneManager.getDefaultType(uri);
            final Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
            MP3File newHandle = attemptDataSource(resolver, actualUri);
            if (newHandle != null) {
                return newHandle;
            } else {
                return open(uri.toString());
            }
        } else {
            // Try requested Uri locally first, or fallback to media server
            MP3File newHandle = attemptDataSource(resolver, uri);
            if (newHandle != null) {
                return newHandle;
            } else {
                return open(uri.toString());
            }
        }
    }

    private MP3File attemptDataSource(ContentResolver resolver, Uri uri) {
        try{
            AssetFileDescriptor afd = resolver.openAssetFileDescriptor(uri, "r");
            return open(afd);
        } catch (NullPointerException | SecurityException | IOException ex) {
            LogHelper.w(TAG, "Couldn't open " + uri + ": " + ex);
            return null;
        }
    }

    public MP3File open(String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

        final Uri uri = Uri.parse(path);
        final String scheme = uri.getScheme();

        MP3File newHandle;
        if ("file".equals(scheme)) {
            path = uri.getPath();
        } else if (scheme != null) {
            // handle non-file sources
            throw new UnsupportedOperationException("I don't wanna deal with this: " + uri);
            /*nativeSetDataSource(
                    MediaHTTPService.createHttpServiceBinderIfNecessary(path),
                    path,
                    keys,
                    values);
            return;
                    */
        }

        final File file = new File(path);
        if (file.exists()) {
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            newHandle = open(fd);
        } else {
            throw new IOException("setDataSource failed.");
        }
        return newHandle;
    }

    public MP3File open(AssetFileDescriptor afd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        // Note: using getDeclaredLength so that our behavior is the same
        // as previous versions when the content provider is returning
        // a full file.
        if (afd.getDeclaredLength() < 0) {
            return open(afd.getParcelFileDescriptor());
        } else {
            return open(afd.getParcelFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
        }
    }

    public MP3File open(ParcelFileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        // intentionally less than LONG_MAX
        return open(fd, 0, 0x7ffffffffffffffL);
    }

    public MP3File open(ParcelFileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        return openFromDataSource(fd.detachFd(), offset, length);
    }
}
