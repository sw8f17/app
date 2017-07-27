package rocks.stalin.android.app.utils.time;

import org.hamcrest.core.IsEqual;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * Created by delusional on 5/4/17.
 */
public class DurationTest {

    @Test
    public void Duration_Instant_fromMicros_Easy() throws Exception {
        Clock.Duration time = Clock.Duration.fromMicros(0);

        assertThat(time.inMillis(), equalTo(0L));
        assertThat(time.getNanos(), equalTo(0));
    }

    @Test
    public void Duration_Instant_fromMicros_1Millis() throws Exception {
        Clock.Duration time = Clock.Duration.fromMicros(1000);

        assertThat(time.inMillis(), equalTo(1L));
        assertThat(time.getNanos(), equalTo(0));
    }

    @Test
    public void Duration_Instant_fromMicros_1Millis_1000Nanos() throws Exception {
        Clock.Duration time = Clock.Duration.fromMicros(1001);

        assertThat(time.inMillis(), equalTo(1L));
        assertThat(time.getNanos(), equalTo(1000));
    }
    @Test
    public void fromNanos_11() throws Exception {
        Clock.Duration d = Clock.Duration.FromNanos(1000001);
        assertThat(d.getMillis(), equalTo(1L));
        assertThat(d.getNanos(), equalTo(1));
    }

    @Test
    public void fromNanos_22() throws Exception {
        Clock.Duration d = Clock.Duration.FromNanos(2000002);
        assertThat(d.getMillis(), equalTo(2L));
        assertThat(d.getNanos(), equalTo(2));
    }

    @Test
    public void fromNanos_noMillis() throws Exception {
        Clock.Duration d = Clock.Duration.FromNanos(2);
        assertThat(d.getMillis(), equalTo(0L));
        assertThat(d.getNanos(), equalTo(2));
    }

    @Test
    public void inSeconds_one() throws Exception {
        Clock.Duration d = new Clock.Duration(1000, 0);
        assertThat(d.inSeconds(), equalTo(1L));
    }

    @Test
    public void inSeconds_moreThanOne() throws Exception {
        Clock.Duration d = new Clock.Duration(1001, 1);
        assertThat(d.inSeconds(), equalTo(1L));
    }

    @Test
    public void inSeconds_lessRoundDown() throws Exception {
        Clock.Duration d = new Clock.Duration(499, 1);
        assertThat(d.inSeconds(), equalTo(0L));
    }

    @Test
    public void inSeconds_lessRoundUp() throws Exception {
        Clock.Duration d = new Clock.Duration(501, 1);
        assertThat(d.inSeconds(), equalTo(1L));
    }

    @Test
    public void inMillis_one() throws Exception {
        Clock.Duration d = new Clock.Duration(1, 0);
        assertThat(d.inMillis(), equalTo(1L));
    }

    @Test
    public void inMillis_moreThanOne() throws Exception {
        Clock.Duration d = new Clock.Duration(1, 1);
        assertThat(d.inMillis(), equalTo(1L));
    }

    @Test
    public void inMillis_roundedDown() throws Exception {
        Clock.Duration d = new Clock.Duration(0, 499999);
        assertThat(d.inMillis(), equalTo(0L));
    }

    @Test
    public void inMillis_roundedUp() throws Exception {
        Clock.Duration d = new Clock.Duration(0, 499999);
        assertThat(d.inMillis(), equalTo(0L));
    }

    @Test
    public void inNanos_one() throws Exception {
        Clock.Duration d = new Clock.Duration(0, 1);
        assertThat(d.inNanos(), equalTo(1L));
    }

    @Test
    public void inNanos_moreThanOne() throws Exception {
        Clock.Duration d = new Clock.Duration(1, 1);
        assertThat(d.inNanos(), equalTo(1000001L));
    }

    @Test
    public void multiply() throws Exception {
        Clock.Duration d = new Clock.Duration(1, 0);
        assertThat(d.multiply(5).inNanos(), equalTo(5000000L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nanosTooBig() throws Exception {
        new Clock.Duration(0L, 1000001);

    }

    @Test
    public void divide_sanityCheck() throws Exception {
        Clock.Duration d = new Clock.Duration(2, 0);
        Clock.Duration half = d.divide(2);
        assertThat(half.getMillis(), equalTo(1L));
        assertThat(half.getNanos(), equalTo(0));
    }

    @Test
    public void divide_overflow() throws Exception {
        Clock.Duration d = new Clock.Duration(3, 0);
        Clock.Duration half = d.divide(2);
        assertThat(half.getMillis(), equalTo(1L));
        assertThat(half.getNanos(), equalTo(500000));
    }

    @Test
    public void divide_overflow_and_nanos() throws Exception {
        Clock.Duration d = new Clock.Duration(3, 500000);
        Clock.Duration half = d.divide(2);
        assertThat(half.getMillis(), equalTo(1L));
        assertThat(half.getNanos(), equalTo(750000));
    }

    @Test
    public void divide_by_three() throws Exception {
        Clock.Duration d = new Clock.Duration(3, 900000);
        Clock.Duration half = d.divide(3);
        assertThat(half.getMillis(), equalTo(1L));
        assertThat(half.getNanos(), equalTo(300000));
    }


}