package rocks.stalin.android.app.playback;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.support.v4.util.LogWriter;

import java.util.concurrent.Semaphore;

import rocks.stalin.android.app.MP3MediaInfo;
import rocks.stalin.android.app.utils.LogHelper;

/**
 * Created by delusional on 4/24/17.
 */

public class LocalSoundSink implements AudioSink {
    public static final String TAG = LogHelper.makeLogTag(LocalSoundSink.class);

    AudioTrack at;
    private MP3MediaInfo mediaInfo;

    private Thread audioThread;
    private Semaphore audioWriteLock;
    private int frameRatio;

    public LocalSoundSink() {
    }

    @Override
    public void change(MP3MediaInfo mediaInfo, final PluggableMediaPlayer.MediaBuffer mediaBuffer) {
        if(at != null)
            throw new IllegalStateException("You have to reset the sink before starting a new playback");
        this.mediaInfo = mediaInfo;
        AudioFormat.Builder format = new AudioFormat.Builder()
                .setSampleRate((int) mediaInfo.sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT);

        switch(mediaInfo.channels) {
            case 1:
                format.setChannelMask(AudioFormat.CHANNEL_OUT_MONO);
                break;
            case 2:
                format.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO);
                break;
        }

        frameRatio = mediaInfo.encoding.getSampleSize() * mediaInfo.channels;

        at = new AudioTrack.Builder()
                .setBufferSizeInBytes((int) (mediaInfo.frameSize*2))
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(format
                        .build()
                )
                .build();

        at.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
                LogHelper.d(TAG, "Time for more data");
                if(audioWriteLock.availablePermits() > 0)
                    LogHelper.w(TAG, "Buffer underflow. It seems like we aren't reading data quickly enough. You might notice pauses");
                audioWriteLock.release();
            }
        });
        //We want to preload the track
        audioWriteLock = new Semaphore(2, true);
        at.setPositionNotificationPeriod((int) (mediaInfo.frameSize/frameRatio));

        audioThread = new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        audioWriteLock.acquire();
                        byte[] buffer = mediaBuffer.read();
                        int written = at.write(buffer, 0, buffer.length, AudioTrack.WRITE_NON_BLOCKING);
                        if(written != buffer.length)
                            LogHelper.w(TAG, "Buffer overflow, discarding the overflowing bytes");
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        audioThread.setName("LocalSoundSink Audio Thread");
        audioThread.start();
    }

    @Override
    public void play() {
        at.play();
    }

    @Override
    public void pause() {
        at.pause();
    }

    @Override
    public void stop() {
        at.pause();
        at.flush();
        at.stop();
    }

    @Override
    public void resume() {
        at.play();
        audioWriteLock.release();
    }

    @Override
    public void reset() {
        stop();
        at.release();
        audioThread.interrupt();
        at = null;
    }
}
