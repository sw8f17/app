package rocks.stalin.android.app.playback;

/**
 * Created by delusional on 4/24/17.
 */

enum PlaybackState {
    Idle,
    Initialized,
    Preparing,
    Prepared,
    Stopped,
    Paused,
    Playing,
    Complete,
    Error,
    End,
}
