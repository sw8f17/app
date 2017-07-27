package rocks.stalin.android.app.playback.actions;

import com.squareup.wire.Message;

import rocks.stalin.android.app.decoding.MediaInfo;
import rocks.stalin.android.app.playback.ActionStrategy;
import rocks.stalin.android.app.proto.Metadata;
import rocks.stalin.android.app.proto.SongChangeCommand;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/10/17.
 */

public class MediaChangeAction extends TimedAction<SongChangeCommand, SongChangeCommand.Builder> {
    private MediaInfo mediaInfo;

    public MediaChangeAction(Clock.Instant time, MediaInfo mediaInfo) {
        super(time);
        this.mediaInfo = mediaInfo;
    }

    @Override
    public void execute(ActionStrategy backend) {
        backend.changeMedia(mediaInfo);
    }

    @Override
    public String name() {
        return "MediaChange";
    }

    @Override
    public Message toMessage() {
        Timestamp time = getTimestampMessage();
        return new SongChangeCommand.Builder()
                .playtime(time)
                .songmetadata(new Metadata.Builder()
                    .mediainfo(new rocks.stalin.android.app.proto.MediaInfo.Builder()
                        .samplerate((int) mediaInfo.sampleRate)
                        .channels(mediaInfo.channels)
                        .framesize((int) mediaInfo.frameSize)
                        .encoding(rocks.stalin.android.app.proto.MediaInfo.Encoding.UNSIGNED16)
                        .build())
                    .build())
                .build();
    }
}
