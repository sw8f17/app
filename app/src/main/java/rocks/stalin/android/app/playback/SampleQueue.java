package rocks.stalin.android.app.playback;

import com.google.android.gms.common.api.TransformedResult;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by delusional on 5/3/17.
 */

public class SampleQueue {
    private TreeMap<Long, byte[]> samples = new TreeMap<>();

    public void putSample(long milliseconds, byte[] sample) {
        samples.put(milliseconds, sample);
    }

    public byte[] getCurrent() {
        Map.Entry<Long, byte[]> entry = samples.lowerEntry(System.currentTimeMillis());
        return entry == null ? null : entry.getValue();
    }

    public long getCurrentKey() {
        Long aLong = samples.lowerKey(System.currentTimeMillis());
        return aLong == null ? -1 : aLong;
    }

    public long getNextKey() {
        return samples.higherKey(System.currentTimeMillis());
    }
}
