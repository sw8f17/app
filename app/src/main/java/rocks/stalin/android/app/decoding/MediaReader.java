package rocks.stalin.android.app.decoding;

import android.content.Context;
import android.database.sqlite.SQLiteClosable;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Format;
import java.util.Map;
import java.util.concurrent.Callable;

import rocks.stalin.android.app.BuildConfig;
import rocks.stalin.android.app.framework.concurrent.TaskExecutor;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;
import rocks.stalin.android.app.framework.concurrent.observable.ObservableFutureTask;
import rocks.stalin.android.app.framework.functional.Consumer;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

public class MediaReader extends MediaCodec.Callback {
    private static final String TAG = LogHelper.makeLogTag(MediaReader.class);

    State state;
    MediaExtractor extractor;
    MediaCodec codec;
    MediaFormat outputFormat;

    private Consumer<Handle> onBufferAvailableCallback;
    private MediaFormat format;

    public MediaReader() {
        state = State.END;

        extractor = null;
        codec = null;
    }

    public void setOnBufferAvailable(Consumer<Handle> callback) {
        this.onBufferAvailableCallback = callback;
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {
        if(BuildConfig.DEBUG && state != State.EMPTY) {
            LogHelper.e(TAG, "Can't set datasource from state ", state);
            throw new IllegalStateException();
        }
        extractor.setDataSource(context, uri, headers);

        state = State.READY;
    }

    public ObservableFuture<MediaInfo> prepare(TaskExecutor executor) {
        if(BuildConfig.DEBUG && state != State.READY) {
            LogHelper.e(TAG, "Can't start from state ", state);
            throw new IllegalStateException();
        }
        state = State.PREPARING;
        ObservableFutureTask<MediaInfo> futureTask = new ObservableFutureTask<>(new Callable<MediaInfo>() {
            @Override
            public MediaInfo call() throws Exception {
                if (BuildConfig.DEBUG && state != State.PREPARING) {
                    LogHelper.e(TAG, "Can't start from state ", state);
                    throw new IllegalStateException();
                }

                // Find the tracks that has mime type of audio/*.
                // We are hoping that we can decode that
                MediaFormat format = null;
                String mime = null;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    format = extractor.getTrackFormat(i);
                    mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("audio/")) {
                        extractor.selectTrack(i);
                        LogHelper.i(TAG, "Selected track with id ", i, " with mime ", mime);
                    }
                }
                if (format == null)
                    throw new RuntimeException("No track found in file");

                MediaCodecList mcList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
                String codecName = mcList.findDecoderForFormat(format);
                codec = MediaCodec.createByCodecName(codecName);
                codec.configure(format, null, null, 0);
                codec.setCallback(MediaReader.this);
                outputFormat = codec.getOutputFormat();

                state = State.PREPARED;

                return MediaInfo.fromFormat(outputFormat);
            }
        });
        executor.submit(futureTask);
        return futureTask;
    }

    public void start() {
        if(BuildConfig.DEBUG && state != State.PREPARED) {
            LogHelper.e(TAG, "Can't start from state ", state);
            throw new IllegalStateException();
        }

        codec.start();

        state = State.RUNNING;
    }

    public void stop() {
        if(BuildConfig.DEBUG && state != State.RUNNING) {
            LogHelper.e(TAG, "Can't stop from state ", state);
            throw new IllegalStateException();
        }

        codec.stop();

        state = State.END;
    }

    public void reset() {
        if(BuildConfig.DEBUG && state != State.END) {
            LogHelper.e(TAG, "Can't reset from state ", state);
            throw new IllegalStateException();
        }

        if(extractor != null)
            extractor.release();
        extractor = new MediaExtractor();

        if(codec != null)
            codec.release();
        codec = null;

        state = State.EMPTY;
    }

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
        ByteBuffer buffer = codec.getInputBuffer(index);

        int read = extractor.readSampleData(buffer, 0);
        if (read == -1) { //No more data
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return;
        }

        codec.queueInputBuffer(index, 0, read, extractor.getSampleTime(), 0);
        extractor.advance();
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
        ByteBuffer buffer = codec.getOutputBuffer(index);
        Handle handle = new Handle(codec, index, info, outputFormat, buffer);
        onBufferAvailableCallback.call(handle);
    }

    @Override
    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
        //TODO
        LogHelper.e(TAG, "There was an error in the media codec");
        e.printStackTrace();
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
        //TODO
    }

    public MediaFormat getFormat() {
        return format;
    }

    private enum State {
        END,
        EMPTY,
        READY,
        PREPARING,
        PREPARED,
        RUNNING,
    }

    public static class Handle implements Comparable<Handle> {
        private final MediaCodec codec;
        protected final int index;
        public final MediaCodec.BufferInfo info;
        private final MediaFormat format;
        public final ByteBuffer buffer;

        protected Handle(MediaCodec codec, int index, MediaCodec.BufferInfo info, MediaFormat format, ByteBuffer buffer) {
            this.codec = codec;
            this.index = index;
            this.info = info;
            this.format = format;
            this.buffer = buffer;
        }

        public void release() {
            codec.releaseOutputBuffer(index, false);
        }

        public long getSampleRate() {
            return format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        }

        public int getChannelCount() {
            return format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        }

        public long getPCMSampleSize() {
            return format.getInteger(MediaFormat.KEY_PCM_ENCODING);
        }

        public Clock.Instant getPresentationOffset() {
            return Clock.Instant.fromMicros(info.presentationTimeUs);
        }

        @Override
        public int compareTo(@NonNull Handle other) {
            // There's a potential bug here for large variations of presentation time.
            // I'll assume the difference isn't that big for now.
            return (int) (this.info.presentationTimeUs - other.info.presentationTimeUs);
        }
    }
}
