package rocks.stalin.android.app.playback;

import java.nio.ByteBuffer;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

interface AudioMixer {
    void pushFrame(MP3MediaInfo mediaInfo, Clock.Instant nextTime, ByteBuffer read);

    void pushAction(TimedAction action);

    TimedAction readAction();
}
