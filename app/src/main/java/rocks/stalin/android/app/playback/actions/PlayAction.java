package rocks.stalin.android.app.playback.actions;

import android.media.AudioTrack;

/**
 * Created by delusional on 5/4/17.
 */

public class PlayAction extends TimedAction {
    public PlayAction(long time) {
        super(time);
    }

    public void execute(AudioTrack at) {
        at.play();
    }
}
