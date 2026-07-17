package com.oikoaudio.fire.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

class TrackSelectIndicatorLightsTest {
    @Test
    void greenUsesTrackSelectGreenStates() {
        assertEquals(BiColorLightState.AMBER_HALF, TrackSelectIndicatorLights.green(false));
        assertEquals(BiColorLightState.AMBER_FULL, TrackSelectIndicatorLights.green(true));
    }

    @Test
    void redUsesTrackSelectRedStates() {
        assertEquals(BiColorLightState.GREEN_HALF, TrackSelectIndicatorLights.red(false));
        assertEquals(BiColorLightState.GREEN_FULL, TrackSelectIndicatorLights.red(true));
    }
}
