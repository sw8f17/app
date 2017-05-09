package rocks.stalin.android.app.utils.time;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * Created by delusional on 5/4/17.
 */
public class DurationTest {
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

}