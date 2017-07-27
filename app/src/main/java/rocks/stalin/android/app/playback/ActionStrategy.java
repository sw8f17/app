package rocks.stalin.android.app.playback;

import rocks.stalin.android.app.decoding.MediaInfo;

public interface ActionStrategy {
    void play();

    void pause();

    void changeMedia(MediaInfo mediaInfo);
}
