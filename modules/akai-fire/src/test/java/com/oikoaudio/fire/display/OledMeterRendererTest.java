package com.oikoaudio.fire.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OledMeterRendererTest {

    @Test
    void verticalMetersStayBlankForSilence() {
        final int[] image = OledMeterRenderer.verticalMeters(new int[]{0, 0}, 2);

        assertTrue(OledMeterRenderer.isBlank(image));
    }

    @Test
    void verticalMetersFillHigherValuesFromBottom() {
        final int[] image = OledMeterRenderer.verticalMeters(new int[]{127}, 1);

        assertEquals(1, OledMeterRenderer.pixel(image, 64, 60));
        assertEquals(1, OledMeterRenderer.pixel(image, 64, 4));
    }

    @Test
    void verticalMetersDrawPeakMarkerAboveCurrentValue() {
        final int[] image = OledMeterRenderer.verticalMeters(new int[]{32}, new int[]{127}, 1);

        assertEquals(1, OledMeterRenderer.pixel(image, 64, 2));
        assertEquals(0, OledMeterRenderer.pixel(image, 64, 20));
    }

    @Test
    void mutedMetersDrawOnlyBottomDash() {
        final int[] image = OledMeterRenderer.verticalMeters(new int[]{127}, new int[]{127}, new boolean[]{true}, 1);

        assertEquals(1, OledMeterRenderer.pixel(image, 64, 60));
        assertEquals(0, OledMeterRenderer.pixel(image, 64, 20));
        assertEquals(0, OledMeterRenderer.pixel(image, 64, 2));
    }

    @Test
    void emptyMeterSetReturnsBlankImage() {
        assertTrue(OledMeterRenderer.isBlank(OledMeterRenderer.verticalMeters(new int[0], 0)));
    }

    @Test
    void largeMeterDrawsFillAndPeakMarkerWithoutFrame() {
        final int[] image = OledMeterRenderer.largeMeter(64, 127);

        assertEquals(0, OledMeterRenderer.pixel(image, 32, 2));
        assertEquals(1, OledMeterRenderer.pixel(image, 64, 58));
        assertEquals(0, OledMeterRenderer.pixel(image, 64, 20));
        assertEquals(1, OledMeterRenderer.pixel(image, 64, 4));
    }

    @Test
    void largeMeterStaysBlankForSilenceWithoutHeldPeak() {
        assertTrue(OledMeterRenderer.isBlank(OledMeterRenderer.largeMeter(0, 0)));
    }
}
