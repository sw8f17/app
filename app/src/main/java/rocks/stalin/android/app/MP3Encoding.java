package rocks.stalin.android.app;

import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by delusional on 4/24/17.
 */

public enum MP3Encoding {
    BIT8(0x000f, 1),
    BIT16(0x0040, 2),
    BIT24(0x4000, 3),
    BIT32(0x0100, 4),
    SIGNED(0x0080, 1),
    FLOAT(0x0E00, 0),
    SIGNED16(BIT16.code|SIGNED.code|0x10, 2),
    UNSIGNED16(BIT16.code|0x20, 2),
    UNSIGNED8(0x0001, 1),
    SIGNED8(SIGNED.code|0x0002, 1),
    ULAW8(0x0004, 1),
    ALAW8(0x0008, 1),
    SIGNED32(BIT32.code|SIGNED.code|0x1000, 4),
    UNSIGNED32(BIT32.code|0x2000, 4),
    SIGNED24(BIT24.code|SIGNED.code|0x1000, 3),
    UNSIGNED24(BIT24.code|0x2000, 3),
    FLOAT32(0x0200, 4),
    FLOAT64(0x0400, 8);

    private int code;
    private int sampleSize;

    private static SparseArray<MP3Encoding> map = new SparseArray<>();

    static {
        for (MP3Encoding legEnum : MP3Encoding.values()) {
            map.put(legEnum.code, legEnum);
        }
    }

    MP3Encoding(final int code, final int sampleSize) {
        this.code = code;
        this.sampleSize = sampleSize;
    }

    public static MP3Encoding valueOf(int code) {
        return map.get(code);
    }

    public int getSampleSize() {
        return sampleSize;
    }
}
