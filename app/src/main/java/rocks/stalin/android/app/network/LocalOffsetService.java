package rocks.stalin.android.app.network;

import rocks.stalin.android.app.utils.time.Clock;

public class LocalOffsetService implements OffsetSource {
    @Override
    public Clock.Duration getOffset() {
        return new Clock.Duration(0L, 0);
    }

    @Override
    public void tearDown() {

    }
}
