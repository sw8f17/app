package rocks.stalin.android.app.playback;

import com.squareup.wire.Message;

import java.nio.ByteBuffer;

import rocks.stalin.android.app.decoding.MediaInfo;
import rocks.stalin.android.app.playback.actions.TimedAction;
import rocks.stalin.android.app.utils.time.Clock;

interface TimedEventQueue {
    void pushBuffer(ByteBuffer copy, Clock.Instant presentationOffset);

    void pushSync(Clock.Instant realTime, Clock.Instant mediaTime);

    <M extends Message<M, B>, B extends Message.Builder<M, B>> void pushAction(TimedAction<M, B> action);


    void release();

}
