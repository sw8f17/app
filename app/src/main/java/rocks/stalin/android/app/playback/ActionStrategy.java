package rocks.stalin.android.app.playback;

import rocks.stalin.android.app.decoding.MP3MediaInfo;

public interface ActionStrategy {
    void play();

    void pause();

    void changeMedia(MP3MediaInfo mediaInfo);
}
