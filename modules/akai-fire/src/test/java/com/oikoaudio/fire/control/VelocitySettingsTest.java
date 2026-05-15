package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocitySettingsTest {
    @Test
    void resolvesRawVelocityThroughDefaultAndSensitivity() {
        final VelocitySettings settings = new VelocitySettings(100, 1, 126, 50);

        assertEquals(114, settings.resolveVelocity(127));
    }

    @Test
    void clampsCenterVelocityToConfiguredRange() {
        final VelocitySettings settings = new VelocitySettings(100, 1, 126, 100);

        assertTrue(settings.adjustCenterVelocity(200));
        assertEquals(126, settings.centerVelocity());
        assertFalse(settings.adjustCenterVelocity(1));
    }

    @Test
    void clampsSensitivityRange() {
        final VelocitySettings settings = new VelocitySettings(100, 1, 126, 100);

        assertTrue(settings.adjustSensitivity(-200));
        assertEquals(0, settings.sensitivity());
        assertFalse(settings.adjustSensitivity(-1));
    }

    @Test
    void resetsToInitialClampedValues() {
        final VelocitySettings settings = new VelocitySettings(200, 1, 126, 140);
        settings.adjustCenterVelocity(-20);
        settings.adjustSensitivity(-40);

        settings.reset();

        assertEquals(126, settings.centerVelocity());
        assertEquals(100, settings.sensitivity());
    }

    @Test
    void rejectsInvalidCenterVelocityRange() {
        assertThrows(IllegalArgumentException.class, () -> new VelocitySettings(100, 126, 1, 100));
    }
}
