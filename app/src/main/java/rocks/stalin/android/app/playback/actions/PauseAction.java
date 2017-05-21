package rocks.stalin.android.app.playback.actions;

import com.squareup.wire.Message;

import rocks.stalin.android.app.network.Messageable;
import rocks.stalin.android.app.playback.ActionStrategy;
import rocks.stalin.android.app.proto.PauseCommand;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public class PauseAction extends TimedAction implements Messageable<PauseCommand, PauseCommand.Builder> {
    public PauseAction(Clock.Instant time) {
        super(time);
    }

    @Override
    public void execute(ActionStrategy backend) {
        backend.pause();
    }

    @Override
    public String name() {
        return "Pause";
    }

    @Override
    public Message toMessage() {
        return new PauseCommand.Builder()
                .playtime(getTimestampMessage())
                .build();
    }
}
