package rocks.stalin.android.app.playback.actions;

import com.squareup.wire.Message;

import rocks.stalin.android.app.decoding.MP3MediaInfo;
import rocks.stalin.android.app.network.Messageable;
import rocks.stalin.android.app.playback.AudioMixer;
import rocks.stalin.android.app.playback.LocalSoundSink;
import rocks.stalin.android.app.proto.MediaInfo;
import rocks.stalin.android.app.proto.Metadata;
import rocks.stalin.android.app.proto.SongChangeCommand;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.time.Clock;

/**
 * Created by delusional on 5/10/17.
 */

public class MediaChangeAction extends TimedAction implements Messageable<SongChangeCommand, SongChangeCommand.Builder> {
    private MP3MediaInfo mediaInfo;

    public MediaChangeAction(Clock.Instant time, MP3MediaInfo mediaInfo) {
        super(time);
        this.mediaInfo = mediaInfo;
    }

    @Override
    public void execute(LocalSoundSink at, AudioMixer mixer) {
        at.reset();
        at.change(mediaInfo);
    }

    @Override
    public String name() {
        return "MediaChange";
    }

    @Override
    public SongChangeCommand toMessage() {
        Timestamp time = getTimestampMessage();
        return new SongChangeCommand.Builder()
                .playtime(time)
                .songmetadata(new Metadata.Builder()
                    .mediainfo(new MediaInfo.Builder()
                        .samplerate((int) mediaInfo.sampleRate)
                        .channels(mediaInfo.channels)
                        .framesize((int) mediaInfo.frameSize)
                        .encoding(MediaInfo.Encoding.UNSIGNED16)
                        .build())
                    .build())
                .build();
    }
}
