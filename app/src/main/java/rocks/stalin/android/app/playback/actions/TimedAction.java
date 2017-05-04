package rocks.stalin.android.app.playback.actions;

import android.media.AudioTrack;
import android.support.annotation.NonNull;

/**
 * Created by delusional on 5/4/17.
 */

public abstract class TimedAction implements Comparable<TimedAction> {
    private long time;

    public TimedAction(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public abstract void execute(AudioTrack at);

    @Override
    public int compareTo(@NonNull TimedAction o) {
        return Long.compare(time, o.getTime());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimedAction that = (TimedAction) o;

        return time == that.time;

    }

    @Override
    public int hashCode() {
        return (int) (time ^ (time >>> 32));
    }
}
