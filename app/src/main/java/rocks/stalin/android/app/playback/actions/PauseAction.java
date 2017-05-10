package rocks.stalin.android.app.playback.actions;

import rocks.stalin.android.app.playback.AudioMixer;
import rocks.stalin.android.app.playback.LocalSoundSink;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public class PauseAction extends TimedAction {
    public PauseAction(Clock.Instant time) {
        super(time);
    }

    @Override
    public void execute(LocalSoundSink at, AudioMixer mixer) {
        at.stop();
        mixer.flush();
    }

    @Override
    public String name() {
        return "Pause";
    }
}
