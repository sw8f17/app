package rocks.stalin.android.app.utils.time;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class InstantTest {

    @Test
    public void Clock_Instant_inMicros_Easy() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 0);

        assertThat(i.inMicros(), equalTo(1000L));
    }

    @Test
    public void Clock_Instant_inMicros_RoundUp() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 500);

        assertThat(i.inMicros(), equalTo(1001L));
    }

    @Test
    public void Clock_Instant_inMicros_RoundDown() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 499);

        assertThat(i.inMicros(), equalTo(1000L));
    }

    @Test
    public void Clock_Instant_sub_Instant_Easy() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 0);
        Clock.Instant i2 = new Clock.Instant(1L, 0);

        Clock.Duration found = i.sub(i2);

        assertThat(found.inMillis(), equalTo(0L));
    }

    @Test
    public void Clock_Instant_sub_Instant_Easy_Negative() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 0);
        Clock.Instant i2 = new Clock.Instant(2L, 0);

        Clock.Duration found = i.sub(i2);

        assertThat(found.inMillis(), equalTo(-1L));
    }

    @Test
    public void Clock_Instant_sub_Instant_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(2L, 0);
        Clock.Instant i2 = new Clock.Instant(0L, 1);

        Clock.Duration found = i.sub(i2);

        assertThat(found.inNanos(), equalTo(1999999L));
    }

    @Test
    public void Clock_Instant_sub_Instant_Rollover_Negative() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 2);
        Clock.Instant i2 = new Clock.Instant(2L, 1);

        Clock.Duration found = i.sub(i2);

        assertThat(found.inNanos(), equalTo(-999999L));
    }

    @Test
    public void Clock_Instant_sub_Instant_REALFAIL1() throws Exception {
        Clock.Instant T0 = new Clock.Instant(1495029840555L, 39079);
        Clock.Instant T1 = new Clock.Instant(1495029838433L, 357761);
        Clock.Instant T2 = new Clock.Instant(1495029838433L, 655261);
        Clock.Instant T3 = new Clock.Instant(1495029840585L, 801683);

        Clock.Duration requestOffset = T1.sub(T0);
        Clock.Duration responseOffset = T2.sub(T3);
        Clock.Duration sum = requestOffset.add(responseOffset);
        Clock.Duration offset = sum.divide(2);

        assertThat(offset.getMillis(), equalTo(-2136L));
    }

    @Test
    public void Clock_Instant_sub_Instant_NoMillis() throws Exception {
        Clock.Instant i = new Clock.Instant(0L, 2);
        Clock.Instant i2 = new Clock.Instant(0L, 1);

        Clock.Duration found = i.sub(i2);

        assertThat(found.inNanos(), equalTo(1L));
    }

    @Test
    public void Clock_Instant_sub_Instant_NoMillis_Negative() throws Exception {
        Clock.Instant i = new Clock.Instant(0L, 1);
        Clock.Instant i2 = new Clock.Instant(0L, 2);

        Clock.Duration found = i.sub(i2);

        assertThat(found.inNanos(), equalTo(-1L));
    }

    @Test
    public void Clock_Instant_add_SanityCheck() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 0);
        Clock.Duration d = new Clock.Duration(1L, 0);

        Clock.Instant found = i.add(d);

        assertThat(found.inMillis(), equalTo(2L));
    }

    @Test
    public void Clock_Instant_add_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 999999);
        Clock.Duration d = new Clock.Duration(1L, 1);

        Clock.Instant found = i.add(d);

        assertThat(found.getMillis(), equalTo(3L));
        assertThat(found.getNanos(), equalTo(0));
        assertThat(found.inMillis(), equalTo(3L));
    }

    @Test
    public void Clock_Instant_add_Exactly_Not_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 999998);
        Clock.Duration d = new Clock.Duration(1L, 1);

        Clock.Instant found = i.add(d);

        assertThat(found.getMillis(), equalTo(2L));
        assertThat(found.getNanos(), equalTo(999999));
        assertThat(found.inMillis(), equalTo(3L));
    }

    @Test
    public void Clock_Instant_add_Midway_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 499999);
        Clock.Duration d = new Clock.Duration(1L, 1);

        Clock.Instant found = i.add(d);

        assertThat(found.getMillis(), equalTo(2L));
        assertThat(found.getNanos(), equalTo(500000));
        assertThat(found.inMillis(), equalTo(3L));
    }

    @Test
    public void Clock_Instant_add_Midway_Not_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 499998);
        Clock.Duration d = new Clock.Duration(1L, 1);

        Clock.Instant found = i.add(d);

        assertThat(found.getMillis(), equalTo(2L));
        assertThat(found.getNanos(), equalTo(499999));
        assertThat(found.inMillis(), equalTo(2L));
    }

    @Test
    public void Clock_Instant_sub_SanityCheck() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 1);
        Clock.Duration d = new Clock.Duration(0L, 1);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(1L));
        assertThat(found.getNanos(), equalTo(0));
        assertThat(found.inMillis(), equalTo(1L));
    }

    @Test
    public void Clock_Instant_sub_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(1L, 1);
        Clock.Duration d = new Clock.Duration(0L, 2);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(0L));
        assertThat(found.getNanos(), equalTo(999999));
        assertThat(found.inMillis(), equalTo(1L));
    }

    @Test
    public void Clock_Instant_sub_Large_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(10L, 1);
        Clock.Duration d = new Clock.Duration(3L, 2);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(6L));
        assertThat(found.getNanos(), equalTo(999999));
        assertThat(found.inMillis(), equalTo(7L));
    }

    @Test
    public void Clock_Instant_sub_Midway_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(10L, 0);
        Clock.Duration d = new Clock.Duration(5L, 500001);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(4L));
        assertThat(found.getNanos(), equalTo(499999));
        assertThat(found.inMillis(), equalTo(4L));
    }

    @Test
    public void Clock_Instant_sub_Midway_Not_Rollover() throws Exception {
        Clock.Instant i = new Clock.Instant(10L, 0);
        Clock.Duration d = new Clock.Duration(5L, 499999);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(4L));
        assertThat(found.getNanos(), equalTo(500001));
        assertThat(found.inMillis(), equalTo(5L));
    }

    @Test
    public void Clock_Instant_sub_Negative_Duration_Millis() throws Exception {
        Clock.Instant i = new Clock.Instant(10L, 0);
        Clock.Duration d = new Clock.Duration(-5L, 0);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(15L));
        assertThat(found.getNanos(), equalTo(0));
    }

    @Test
    public void Clock_Instant_sub_Negative_Duration_Nanos() throws Exception {
        Clock.Instant i = new Clock.Instant(10L, 0);
        Clock.Duration d = new Clock.Duration(-0L, -100);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(10L));
        assertThat(found.getNanos(), equalTo(100));
    }

    @Test
    public void Clock_Instant_sub_Negative_Duration_Nanos_Large() throws Exception {
        Clock.Instant i = new Clock.Instant(9L, 999900);
        Clock.Duration d = new Clock.Duration(-0L, -100);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(10L));
        assertThat(found.getNanos(), equalTo(0));
    }

    @Test
    public void Clock_Instant_sub_Negative_Duration_Nanos_Large_above() throws Exception {
        Clock.Instant i = new Clock.Instant(9L, 999901);
        Clock.Duration d = new Clock.Duration(-0L, -100);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(10L));
        assertThat(found.getNanos(), equalTo(1));
    }

    @Test
    public void Clock_Instant_sub_Negative_Duration_Nanos_Large_below() throws Exception {
        Clock.Instant i = new Clock.Instant(9L, 999899);
        Clock.Duration d = new Clock.Duration(-0L, -100);

        Clock.Instant found = i.sub(d);

        assertThat(found.getMillis(), equalTo(9L));
        assertThat(found.getNanos(), equalTo(999999));
    }

    @Test(expected = IllegalArgumentException.class)
    public void Clock_Instant_NanosOverflow() throws Exception {
        new Clock.Instant(0L, 1000001);
    }
}