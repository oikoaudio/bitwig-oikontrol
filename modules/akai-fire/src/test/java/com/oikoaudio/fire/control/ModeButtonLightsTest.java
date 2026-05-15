package com.oikoaudio.fire.control;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

final class ModeButtonLightsTest {
    @Test
    void modeSlotsUseDistinctHardwareStates() {
        assertNotEquals(ModeButtonLights.MODE_1, ModeButtonLights.MODE_2);
        assertNotEquals(ModeButtonLights.MODE_1, ModeButtonLights.MODE_3);
        assertNotEquals(ModeButtonLights.MODE_2, ModeButtonLights.MODE_3);
    }
}
