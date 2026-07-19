package com.oikoaudio.fire.multiclip;

import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

/** Light-state priority for the focused Multiclip Lane Clip pattern. */
final class MulticlipPatternLight {
    private MulticlipPatternLight() {}

    static RgbLightState render(
            final RgbLightState color,
            final boolean withinLoop,
            final boolean held,
            final boolean consumed,
            final boolean playing,
            final boolean occupied,
            final boolean shiftedPlayStart) {
        if (!withinLoop) {
            return RgbLightState.OFF;
        }
        if (held) {
            return consumed ? RgbLightState.PURPLE : color.getBrightest();
        }
        if (playing) {
            return RgbLightState.WHITE;
        }
        if (shiftedPlayStart) {
            return StepPadLightHelper.renderClipStartIndicator(color);
        }
        return occupied ? color.getBrightend() : color.getVeryDimmed();
    }
}
