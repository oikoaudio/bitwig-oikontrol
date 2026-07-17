package com.oikoaudio.fire.control;

import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Visual palette for the four TRACK_SELECT status indicators beside the mute buttons.
 *
 * <p>The Akai Fire uses the same numeric states differently across LED banks. On these indicators,
 * the states named AMBER in BiColorLightState are the green-looking states observed on hardware,
 * while the states named GREEN are the red-looking states.
 */
public final class TrackSelectIndicatorLights {
    private TrackSelectIndicatorLights() {}

    public static BiColorLightState green(final boolean active) {
        return active ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
    }

    public static BiColorLightState red(final boolean active) {
        return active ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    public static BiColorLightState off() {
        return BiColorLightState.OFF;
    }
}
