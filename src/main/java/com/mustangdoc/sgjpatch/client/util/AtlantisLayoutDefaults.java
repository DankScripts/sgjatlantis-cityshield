package com.mustangdoc.sgjpatch.client.util;

/** Locked R13 per-button layout offsets. */
public final class AtlantisLayoutDefaults {
    private static final int[] DX = {
            -17, 111, -30, -27, 5, -12, 9, 0, -68, 11,
            -31, 0, 46, -77, -6, -25, -18, -38, 59, -17,
            -14, 31, -62, -41, -39, -15, -5, -1, -29, -26,
            -40, -4, 3, -41, -88, -3, -27, -65
    };
    private static final int[] DY = {
            -149, -106, -19, -16, 46, 1, 26, 8, -38, -41,
            -20, -20, 67, 26, 67, 26, 2, 45, -18, 4,
            4, 5, 5, 48, 48, 2, 27, 23, 26, 24,
            45, 23, 15, 46, 46, 67, 26, 5
    };

    private AtlantisLayoutDefaults() {}

    public static int dx(int index) {
        return index >= 0 && index < DX.length ? DX[index] : 0;
    }

    public static int dy(int index) {
        return index >= 0 && index < DY.length ? DY[index] : 0;
    }

    public static boolean flip(int index) {
        return index == 6 || index == 8 || index == 11 || index == 12 || index == 13;
    }
}
