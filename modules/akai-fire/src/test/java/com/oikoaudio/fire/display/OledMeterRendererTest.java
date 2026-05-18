package com.oikoaudio.fire.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OledMeterRendererTest {

    @Test
    void verticalMetersDrawBarOutlinesEvenForSilence() {
        final int[] image = OledMeterRenderer.verticalMeters(new int[]{0, 0}, 2);

        assertFalse(OledMeterRenderer.isBlank(image));
        assertEquals(1, OledMeterRenderer.pixel(image, 1, 2));
    }

    @Test
    void verticalMetersFillHigherValuesFromBottom() {
        final int[] image = OledMeterRenderer.verticalMeters(new int[]{127}, 1);

        assertEquals(1, OledMeterRenderer.pixel(image, 64, 60));
        assertEquals(1, OledMeterRenderer.pixel(image, 64, 4));
    }

    @Test
    void emptyMeterSetReturnsBlankImage() {
        assertTrue(OledMeterRenderer.isBlank(OledMeterRenderer.verticalMeters(new int[0], 0)));
    }
}
