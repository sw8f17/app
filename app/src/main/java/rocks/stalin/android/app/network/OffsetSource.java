package rocks.stalin.android.app.network;

import rocks.stalin.android.app.framework.Lifecycle;
import rocks.stalin.android.app.utils.time.Clock;

public interface OffsetSource extends Lifecycle {
    Clock.Duration getOffset();
}
