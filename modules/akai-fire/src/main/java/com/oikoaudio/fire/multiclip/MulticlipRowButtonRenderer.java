package com.oikoaudio.fire.multiclip;

import com.oikoaudio.fire.lights.BiColorLightState;

public final class MulticlipRowButtonRenderer {
    private MulticlipRowButtonRenderer() {}

    public static BiColorLightState render(
            final boolean exists, final boolean active, final boolean muted, final boolean soloed) {
        if (!exists) {
            return BiColorLightState.OFF;
        }
        if (soloed) {
            return BiColorLightState.AMBER_FULL;
        }
        if (muted) {
            return BiColorLightState.GREEN_HALF;
        }
        return BiColorLightState.GREEN_FULL;
    }
}
