package com.oikoaudio.fire;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemotePageNavigationTest {

    @Test
    void remotePageIndexClampsToAvailablePages() {
        assertEquals(1, AkaiFireOikontrolExtension.remotePageIndexAfterTurn(0, 4, 1));
        assertEquals(3, AkaiFireOikontrolExtension.remotePageIndexAfterTurn(2, 4, 8));
        assertEquals(0, AkaiFireOikontrolExtension.remotePageIndexAfterTurn(2, 4, -8));
        assertEquals(2, AkaiFireOikontrolExtension.remotePageIndexAfterTurn(2, 0, 1));
    }

    @Test
    void remotePageCountLabelOnlyShowsWhenThereAreAlternatives() {
        assertEquals("", AkaiFireOikontrolExtension.remotePageCountLabel(0, 1));
        assertEquals("1/7", AkaiFireOikontrolExtension.remotePageCountLabel(0, 7));
        assertEquals("7/7", AkaiFireOikontrolExtension.remotePageCountLabel(6, 7));
    }

    @Test
    void remotePageNavigationLightsShowAvailableDirection() {
        assertEquals(BiColorLightState.OFF,
                AkaiFireOikontrolExtension.remotePageNavigationLightState(0, 1, 1));
        assertEquals(BiColorLightState.OFF,
                AkaiFireOikontrolExtension.remotePageNavigationLightState(0, 4, -1));
        assertEquals(BiColorLightState.AMBER_HALF,
                AkaiFireOikontrolExtension.remotePageNavigationLightState(0, 4, 1));
        assertEquals(BiColorLightState.AMBER_HALF,
                AkaiFireOikontrolExtension.remotePageNavigationLightState(3, 4, -1));
        assertEquals(BiColorLightState.OFF,
                AkaiFireOikontrolExtension.remotePageNavigationLightState(3, 4, 1));
    }
}
