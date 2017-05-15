package rocks.stalin.android.app.utils.time;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class InstantTest {
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

    @Test(expected = IllegalArgumentException.class)
    public void Clock_Instant_NanosOverflow() throws Exception {
        new Clock.Instant(0L, 1000001);
    }
}