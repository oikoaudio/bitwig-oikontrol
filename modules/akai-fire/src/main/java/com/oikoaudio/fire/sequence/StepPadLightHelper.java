package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.RgbLigthState;

public final class StepPadLightHelper {
    private static final int PLAYHEAD_WHITE_FLOOR = 96;

    private StepPadLightHelper() {
    }

    public static RgbLigthState renderEmptyStep(final int stepIndex, final int playingStep) {
        if (stepIndex == playingStep) {
            return RgbLigthState.WHITE;
        }
        return (stepIndex / 4) % 2 == 0 ? RgbLigthState.GRAY_1 : RgbLigthState.GRAY_2;
    }

    public static RgbLigthState renderOccupiedStep(final RgbLigthState base, final boolean accented,
                                                   final boolean playing) {
        if (accented) {
            return base.getBrightend();
        }
        if (playing) {
            return base.getBrightend();
        }
        return base;
    }

    public static RgbLigthState renderPlayheadHighlight(final RgbLigthState base) {
        final RgbLigthState brightest = base.getBrightest();
        return new RgbLigthState(
                Math.max(brightest.getRed() & 0xFF, PLAYHEAD_WHITE_FLOOR),
                Math.max(brightest.getGreen() & 0xFF, PLAYHEAD_WHITE_FLOOR),
                Math.max(brightest.getBlue() & 0xFF, PLAYHEAD_WHITE_FLOOR),
                true);
    }
}
