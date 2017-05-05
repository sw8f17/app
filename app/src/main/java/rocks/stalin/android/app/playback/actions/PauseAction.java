package rocks.stalin.android.app.playback.actions;

import android.media.AudioTrack;

import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public class PauseAction extends TimedAction {
    public PauseAction(Clock.Instant time) {
        super(time);
    }

    @Override
    public void execute(AudioTrack at) {
        at.pause();
    }
}
