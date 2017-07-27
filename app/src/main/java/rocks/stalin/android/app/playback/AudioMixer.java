package rocks.stalin.android.app.playback;

import java.nio.ByteBuffer;

import rocks.stalin.android.app.decoding.MediaInfo;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public interface AudioMixer {
    void pushFrame(MediaInfo mediaInfo, Clock.Instant nextTime, ByteBuffer read);

    void flush();
    void change(MediaInfo mediaInfo);

    void setStartTime(Clock.Instant time);
}
