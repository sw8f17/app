package rocks.stalin.android.app.playback.actions;

import rocks.stalin.android.app.playback.AudioMixer;
import rocks.stalin.android.app.playback.LocalSoundSink;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public class PlayAction extends TimedAction<PlayCommand, PlayCommand.Builder> {
    public PlayAction(Clock.Instant time) {
        super(time);
    }

    public void execute(LocalSoundSink at, AudioMixer mixer) {
        at.play();
    }

    @Override
    public String name() {
        return "Play";
    }

    @Override
    public PlayCommand serialize() {
        return new PlayCommand.Builder()
                .playtime(getTimestampMessage())
                .build();
    }
}
