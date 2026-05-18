package com.oikoaudio.fire;

final class ArrangerGridStep {
    private static final double[] ZOOM_LIMITS = {
            8.8, 27.94, 279.11, 661.61, 1176.20, 2091.03, 4956.52, 8811.59,
            20886.75, 37132.00, 66012.45, 156473.96, 278175.93, 600000.0, 800000.0
    };
    private static final double[] STEPS = {
            Double.NaN, Double.NaN, 1.0, 1.0 / 4.0, 1.0 / 16.0, 1.0 / 32.0,
            1.0 / 64.0, 1.0 / 128.0, 1.0 / 256.0, 1.0 / 512.0,
            1.0 / 1024.0, 1.0 / 2048.0, 1.0 / 4096.0, 1.0 / 8192.0,
            1.0 / 16384.0
    };
    private static final double FOUR_BAR_ZOOM_LIMIT = 14.0;

    private ArrangerGridStep() {
    }

    static double fallbackBeatStep(final int denominator) {
        return Math.max(0.125, 4.0 / Math.max(1, denominator));
    }

    static double barStep(final int numerator, final int denominator) {
        return Math.max(fallbackBeatStep(denominator),
                Math.max(1, numerator) * 4.0 / Math.max(1, denominator));
    }

    static double phraseStep(final int numerator, final int denominator, final int bars) {
        return barStep(numerator, denominator) * Math.max(1, bars);
    }

    static double fromContentPerPixel(final double contentPerPixel,
                                      final int numerator,
                                      final int denominator) {
        if (contentPerPixel <= 0.0) {
            return fallbackBeatStep(denominator);
        }
        final double inverseContentPerPixel = 1.0 / contentPerPixel;
        for (int i = 0; i < ZOOM_LIMITS.length; i++) {
            if (inverseContentPerPixel < ZOOM_LIMITS[i]) {
                if (i == 0) {
                    return phraseStep(numerator, denominator, 8);
                }
                if (i == 1) {
                    return inverseContentPerPixel < FOUR_BAR_ZOOM_LIMIT
                            ? phraseStep(numerator, denominator, 4)
                            : barStep(numerator, denominator);
                }
                return STEPS[i];
            }
        }
        return STEPS[STEPS.length - 1];
    }
}
