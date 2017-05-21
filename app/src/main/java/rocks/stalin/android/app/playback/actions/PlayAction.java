package rocks.stalin.android.app.playback.actions;

import com.squareup.wire.Message;

import rocks.stalin.android.app.network.Messageable;
import rocks.stalin.android.app.playback.ActionStrategy;
import rocks.stalin.android.app.proto.PlayCommand;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/4/17.
 */

public class PlayAction extends TimedAction<PlayCommand, PlayCommand.Builder>{
    public PlayAction(Clock.Instant time) {
        super(time);
    }

    @Override
    public void execute(ActionStrategy backend) {
        backend.play();
    }

    @Override
    public String name() {
        return "Play";
    }

    @Override
    public Message toMessage() {
        return new PlayCommand.Builder()
                .playtime(getTimestampMessage())
                .build();
    }
}
