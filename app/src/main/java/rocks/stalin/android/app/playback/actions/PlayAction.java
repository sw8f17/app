package rocks.stalin.android.app.playback.actions;

import android.media.AudioTrack;

import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public class PlayAction extends TimedAction {
    public PlayAction(Clock.Instant time) {
        super(time);
    }

    public void execute(AudioTrack at) {
        at.play();
    }
}
