package rocks.stalin.android.app.playback.actions;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.playback.LocalSoundSink;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/10/17.
 */

public class MediaChangeAction  extends TimedAction {
    private MP3MediaInfo mediaInfo;

    public MediaChangeAction(Clock.Instant time, MP3MediaInfo mediaInfo) {
        super(time);
        this.mediaInfo = mediaInfo;
    }

    @Override
    public void execute(LocalSoundSink at) {
        at.change(mediaInfo);
    }

    @Override
    public String name() {
        return "MediaChange";
    }
}
