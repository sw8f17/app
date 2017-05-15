package rocks.stalin.android.app.network;

import rocks.stalin.android.app.utils.time.Clock;

public interface OffsetSource {
    Clock.Duration getOffset();
}
