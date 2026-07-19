package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.RgbLightState;
import org.junit.jupiter.api.Test;

class MulticlipLaneLightTest {
    private final RgbLightState color = new RgbLightState(60, 90, 30, true);

    @Test
    void matchesDrumXoxIntensityForOrdinarySelectedAndUnselectedLanes() {
        assertEquals(
                color.getBrightest(),
                MulticlipSequenceMode.laneLightState(color, true, false, false, false, false));
        assertEquals(
                color.getSoftDimmed(),
                MulticlipSequenceMode.laneLightState(color, false, false, false, false, false));
    }

    @Test
    void reservesVeryDimmedForMutedOrExcludedSoloLanes() {
        assertEquals(
                color.getVeryDimmed(),
                MulticlipSequenceMode.laneLightState(color, false, true, true, false, false));
        assertEquals(
                color.getVeryDimmed(),
                MulticlipSequenceMode.laneLightState(color, false, false, false, true, false));
    }
}
