package rocks.stalin.android.app.playback;

import rocks.stalin.android.app.playback.actions.TimedAction;

public class MediaPlayerBackend {
    private AudioMixer mixer;
    private AudioSink sink;

    public MediaPlayerBackend(AudioMixer mixer, AudioSink sink){
        this.mixer = mixer;
        this.sink = sink;
    }

    public void pushAction(TimedAction action) {
    }
}
