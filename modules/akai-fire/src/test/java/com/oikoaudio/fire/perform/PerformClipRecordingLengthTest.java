package com.oikoaudio.fire.perform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformClipRecordingLengthTest {

    @Test
    void roundsSlightlyLateFourFourRecordingsDownToThePreviousBar() {
        assertEquals(16.0, PerformClipLauncherMode.roundToNearestBar(16.1, 4, 4));
    }

    @Test
    void roundsFourFourRecordingsToTheNearestBar() {
        assertEquals(20.0, PerformClipLauncherMode.roundToNearestBar(18.5, 4, 4));
        assertEquals(16.0, PerformClipLauncherMode.roundToNearestBar(17.4, 4, 4));
    }

    @Test
    void respectsTransportMeterWhenRounding() {
        assertEquals(9.0, PerformClipLauncherMode.roundToNearestBar(7.6, 6, 8));
    }

    @Test
    void keepsRoundedLengthAtLeastOneBar() {
        assertEquals(4.0, PerformClipLauncherMode.roundToNearestBar(0.3, 4, 4));
        assertEquals(3.0, PerformClipLauncherMode.roundToNearestBar(Double.NaN, 6, 8));
    }
}
