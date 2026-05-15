package com.oikoaudio.fire.control;

import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Shared palette for repeated top-level mode button presses.
 *
 * The Akai Fire mode buttons expose a small button LED state set rather than
 * arbitrary RGB colors, so mode slots must use visibly distinct hardware states.
 */
public final class ModeButtonLights {
    public static final BiColorLightState MODE_1 = BiColorLightState.GREEN_FULL;
    public static final BiColorLightState MODE_2 = BiColorLightState.AMBER_FULL;
    public static final BiColorLightState MODE_3 = BiColorLightState.RED_HALF;

    private ModeButtonLights() {
    }
}
