package rocks.stalin.android.app.playback.actions;

import android.support.annotation.NonNull;

import com.squareup.wire.Message;

import rocks.stalin.android.app.network.Messageable;
import rocks.stalin.android.app.playback.ActionStrategy;
import rocks.stalin.android.app.playback.AudioMixer;
import rocks.stalin.android.app.playback.MediaPlayer;
import rocks.stalin.android.app.playback.MediaPlayerBackend;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public abstract class TimedAction<M extends Message<M, B>, B extends Message.Builder<M, B>> implements Comparable<TimedAction>, Messageable<M, B> {
    private Clock.Instant time;

    public TimedAction(Clock.Instant time) {
        this.time = time;
    }

    public Clock.Instant getTime() {
        return time;
    }

    public abstract void execute(ActionStrategy backend);
    public abstract String name();

    protected Timestamp getTimestampMessage() {
        return new Timestamp.Builder()
                .millis(time.getMillis())
                .nanos(time.getNanos())
                .build();
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public int compareTo(@NonNull TimedAction o) {
        return time.compareTo(o.getTime());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimedAction that = (TimedAction) o;

        return time.equals(that.time);

    }

    @Override
    public int hashCode() {
        return time.hashCode();
    }
}
