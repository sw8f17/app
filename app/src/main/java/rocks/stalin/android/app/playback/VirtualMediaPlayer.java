package rocks.stalin.android.app.playback;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import rocks.stalin.android.app.decoding.MP3File;
import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.framework.concurrent.TaskScheduler;
import rocks.stalin.android.app.framework.concurrent.TimeAwareRunnable;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;

class VirtualMediaPlayer implements Lifecycle, TimeAwareRunnable {
    private static final String TAG = LogHelper.makeLogTag(VirtualMediaPlayer.class);
    public static final int PRELOAD_SIZE = 7;
    private final TaskScheduler scheduler;

    private ScheduledFuture<?> taskHandle = null;
    private boolean running = false;

    private MP3File file;
    private MP3MediaInfo mediaInfo;
    private AudioMixer player;
    private List<AudioMixer> slaves;
    private long nextSample;
    private Clock.Instant nextFrameStart;

    public VirtualMediaPlayer(MP3File file, MP3MediaInfo mediaInfo, AudioMixer player, List<AudioMixer> slaves, TaskScheduler scheduler) {
        this.scheduler = scheduler;

        this.file = file;
        this.mediaInfo = mediaInfo;
        this.player = player;
        this.slaves = slaves;
    }

    public void setStartTime(Clock.Instant instant) {
        nextFrameStart = instant;
    }

    public long tellAt(Clock.Instant time) {
        if(nextFrameStart == null)
            return 0;
        Clock.Duration diff = time.timeBetween(nextFrameStart);
        long expectedSamples = mediaInfo.bytesPlayedInTime(diff) / mediaInfo.getSampleSize();

        if (time.before(nextFrameStart))
            return nextSample - expectedSamples;

        return nextSample + expectedSamples;
    }

    @Override
    public void run() {
        LogHelper.i(TAG, "Feeding the audio player");

        Clock.Instant now = Clock.getTime();

        Clock.Duration frameTime = mediaInfo.timeToPlayBytes(mediaInfo.frameSize);
        Clock.Duration preloadBufferSize = frameTime.multiply(PRELOAD_SIZE);
        Clock.Instant bufferEnd = now.add(preloadBufferSize);

        while (nextFrameStart.before(bufferEnd)) {
            LogHelper.i(TAG, "Inserting frame into the buffer for playback at ", nextFrameStart);
            ByteBuffer read = file.decodeFrame();
            ByteBuffer left = ByteBuffer.allocate(read.limit()/mediaInfo.channels);
            ByteBuffer right = ByteBuffer.allocate(read.limit()/mediaInfo.channels);

            MP3MediaInfo cMI = new MP3MediaInfo(mediaInfo.sampleRate, 1, mediaInfo.frameSize / mediaInfo.channels, mediaInfo.encoding);

            for(int i = read.position(); i < read.limit(); i += mediaInfo.getSampleSize()) {
                for(int j = 0; j < mediaInfo.encoding.getSampleSize(); j++)
                    left.put(read.get());
                for(int j = 0; j < mediaInfo.encoding.getSampleSize(); j++)
                    right.put(read.get());
            }

            left.flip();
            right.flip();

            //Make sure we are still supposed to be running before pushing the decoded frame
            if(!isRunning())
                break;

            player.pushFrame(cMI, nextFrameStart, left);
            for (AudioMixer slave : slaves)
                slave.pushFrame(cMI, nextFrameStart, right);
            nextSample = file.tell();
            nextFrameStart = nextFrameStart.add(mediaInfo.timeToPlayBytes(read.limit()));
        }
    }

    @Override
    public void start() {
        long period = mediaInfo.timeToPlayBytes(mediaInfo.frameSize).inMillis();
        taskHandle = scheduler.submitWithFixedRate(this, period, TimeUnit.MILLISECONDS);
        running = true;
    }

    @Override
    public void stop() {
        if(taskHandle != null)
            taskHandle.cancel(true);
        taskHandle = null;
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isLongRunning() {
        return true;
    }
}
