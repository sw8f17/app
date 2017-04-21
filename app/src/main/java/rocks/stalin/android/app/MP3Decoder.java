package rocks.stalin.android.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/10/17.
 */

public class MP3Decoder {
    private static final String TAG = LogHelper.makeLogTag(MP3Decoder.class);

    private static native void static_init();

    public native void init();
    public native void exit();

    public native long open(String URI);
    public native void close(long handle);

    public native byte[] decodeFrame(long handle);

    private native long openFromDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException;

    static {
        System.loadLibrary("native-lib");
        static_init();
    }

    /**
     * Sets the data source as a content Uri.
     *
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @throws IllegalStateException if it is called in an invalid state
     */
    public long setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException,
            SecurityException, IllegalStateException {
        final ContentResolver resolver = context.getContentResolver();
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            return setDataSource(uri.getPath());
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                && Settings.AUTHORITY.equals(uri.getAuthority())) {
            // Try cached ringtone first since the actual provider may not be
            // encryption aware, or it may be stored on CE media storage
            final int type = RingtoneManager.getDefaultType(uri);
            final Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
            long newHandle = attemptDataSource(resolver, actualUri);
            if (newHandle != 0) {
                return newHandle;
            } else {
                return setDataSource(uri.toString());
            }
        } else {
            // Try requested Uri locally first, or fallback to media server
            long newHandle = attemptDataSource(resolver, uri);
            if (newHandle != 0) {
                return newHandle;
            } else {
                return setDataSource(uri.toString());
            }
        }
    }

    private long attemptDataSource(ContentResolver resolver, Uri uri) {
        AssetFileDescriptor afd = null;
        try{
            afd = resolver.openAssetFileDescriptor(uri, "r");
            long res = setDataSource(afd);
            afd.close();
            return res;
        } catch (NullPointerException | SecurityException | IOException ex) {
            LogHelper.w(TAG, "Couldn't open " + uri + ": " + ex);
            if(afd != null)
                try {
                    afd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            return 0;
        }
    }

    /**
     * Sets the data source (file-path or http/rtsp URL) to use.
     *
     * @param path the path of the file, or the http/rtsp URL of the stream you want to play
     * @throws IllegalStateException if it is called in an invalid state
     *
     * <p>When <code>path</code> refers to a local file, the file may actually be opened by a
     * process other than the calling application.  This implies that the pathname
     * should be an absolute path (as any other process runs with unspecified current working
     * directory), and that the pathname should reference a world-readable file.
     * As an alternative, the application could first open the file for reading,
     * and then use the file descriptor form {@link #setDataSource(FileDescriptor)}.
     */
    public long setDataSource(String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

        final Uri uri = Uri.parse(path);
        final String scheme = uri.getScheme();

        long newHandle;
        if ("file".equals(scheme)) {
            path = uri.getPath();
        } else if (scheme != null) {
            // handle non-file sources
            throw new UnsupportedOperationException("I don't wanna deal with this");
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
            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            newHandle = setDataSource(fd);
            is.close();
        } else {
            throw new IOException("setDataSource failed.");
        }
        return newHandle;
    }

    /**
     * Sets the data source (AssetFileDescriptor) to use. It is the caller's
     * responsibility to close the file descriptor. It is safe to do so as soon
     * as this call returns.
     *
     * @param afd the AssetFileDescriptor for the file you want to play
     * @throws IllegalStateException if it is called in an invalid state
     * @throws IllegalArgumentException if afd is not a valid AssetFileDescriptor
     * @throws IOException if afd can not be read
     */
    public long setDataSource(AssetFileDescriptor afd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        // Note: using getDeclaredLength so that our behavior is the same
        // as previous versions when the content provider is returning
        // a full file.
        if (afd.getDeclaredLength() < 0) {
            return setDataSource(afd.getFileDescriptor());
        } else {
            return setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
        }
    }

    /**
     * Sets the data source (FileDescriptor) to use. It is the caller's responsibility
     * to close the file descriptor. It is safe to do so as soon as this call returns.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @throws IllegalStateException if it is called in an invalid state
     * @throws IllegalArgumentException if fd is not a valid FileDescriptor
     * @throws IOException if fd can not be read
     */
    public long setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        // intentionally less than LONG_MAX
        return setDataSource(fd, 0, 0x7ffffffffffffffL);
    }

    /**
     * Sets the data source (FileDescriptor) to use.  The FileDescriptor must be
     * seekable (N.B. a LocalSocket is not seekable). It is the caller's responsibility
     * to close the file descriptor. It is safe to do so as soon as this call returns.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @param offset the offset into the file where the data to be played starts, in bytes
     * @param length the length in bytes of the data to be played
     * @throws IllegalStateException if it is called in an invalid state
     * @throws IllegalArgumentException if fd is not a valid FileDescriptor
     * @throws IOException if fd can not be read
     */
    public long setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        return openFromDataSource(fd, offset, length);
    }
}
