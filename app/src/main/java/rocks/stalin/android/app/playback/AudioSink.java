package rocks.stalin.android.app.playback;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaSync;
import android.media.MediaTimestamp;
import android.media.PlaybackParams;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

import rocks.stalin.android.app.decoding.MediaInfo;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

public class AudioSink {
    private static final String TAG = LogHelper.makeLogTag(AudioSink.class);
    private MediaSync mediaSync = null;
    private AudioTrack currentAudioTrack = null;

    public AudioSink() {
    }

    private class MediaSyncCallback extends MediaSync.Callback {
        @Override
        public void onAudioBufferConsumed(@NonNull MediaSync sync, @NonNull ByteBuffer audioBuffer, int bufferId) {
            //TODO: Do we actually need to know? Since we already own all buffers why do we care
        }
    }

    public void queueAudio(ByteBuffer buffer, Clock.Instant timestamp) {
        mediaSync.queueAudio(buffer, 0, timestamp.inMicros());
    }

    public void addSync(Clock.Instant realTimeSync, Clock.Instant mediaTimeSync) {
        MediaTimestamp timestamp = mediaSync.getTimestamp();
        if(timestamp == null)
            return;
        Clock.Instant mediaTime = Clock.Instant.fromMicros(timestamp.getAnchorMediaTimeUs());
        Clock.Instant realTime = Clock.fromNanos(timestamp.getAnchorSytemNanoTime());

        LogHelper.i(TAG, "Realtime is ", realTime, " Sync time is  ", realTimeSync, " making the offset ", realTime.sub(realTimeSync));
        Clock.Instant mediaTimeSyncScaled = mediaTimeSync.add(realTime.sub(realTimeSync));
        LogHelper.i(TAG, "My media time is ", mediaTime, " But i was supposed to be at ", mediaTimeSync);
        float scaleFactor = mediaTimeSyncScaled.inMicros() / (float) mediaTime.inMicros();
        LogHelper.i(TAG, "The scale factor is ", scaleFactor);
        mediaSync.setPlaybackParams(new PlaybackParams().setSpeed(scaleFactor));
    }

    public void play() {
        LogHelper.i(TAG, "Starting playback");
        mediaSync.setPlaybackParams(new PlaybackParams().setSpeed(1.f));
    }

    public void pause() {
        mediaSync.setPlaybackParams(new PlaybackParams().setSpeed(0.f));
    }

    public void reset() {
        if(mediaSync != null) {
            mediaSync.release();
            mediaSync = null;
        }

        if(currentAudioTrack != null) {
            currentAudioTrack.release();
            currentAudioTrack = null;
        }
    }

    public void setMediaType(MediaInfo mediaInfo) {
        if(currentAudioTrack != null)
            throw new IllegalStateException("You can't change media params before a reset");

        AudioFormat.Builder format = new AudioFormat.Builder()
                .setSampleRate((int) mediaInfo.sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT);

        int channelMask;
        switch(mediaInfo.channels) {
            case 1:
                channelMask = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2:
                channelMask = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            default:
                throw new IllegalArgumentException("Channel mode unsupported " + mediaInfo.channels);
        }
        format.setChannelMask(channelMask);

        int minBuffer = AudioTrack.getMinBufferSize((int) mediaInfo.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
        minBuffer = minBuffer *  20; //Allow for up to 20 times slowdown

        currentAudioTrack = new AudioTrack.Builder()
                .setBufferSizeInBytes(minBuffer)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(format
                        .build()
                )
                .build();

        mediaSync = new MediaSync();
        mediaSync.setCallback(new MediaSyncCallback(), null);
        mediaSync.setAudioTrack(currentAudioTrack);
        mediaSync.setOnErrorListener(new MediaSync.OnErrorListener() {
            @Override
            public void onError(@NonNull MediaSync sync, int what, int extra) {
                LogHelper.e("Error in mediasync: ", what, " extra: ", extra);
            }
        }, null);
    }

    public void release() {
        if(mediaSync != null) {
            mediaSync.release();
            mediaSync = null;
        }
        if(currentAudioTrack != null) {
            currentAudioTrack.release();
            currentAudioTrack = null;
        }
    }
}
