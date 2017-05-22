package rocks.stalin.android.app.framework;

import rocks.stalin.android.app.utils.time.Clock;

public interface SimpleMath<SELF extends SimpleMath<SELF>> {
    SELF add(SELF operand);
    SELF sub(SELF operand);

    SELF multiply(int times);

    SELF divide(int denominator);
}
