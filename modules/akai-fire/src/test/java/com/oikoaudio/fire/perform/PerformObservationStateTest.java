package com.oikoaudio.fire.perform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oikoaudio.fire.lights.RgbLightState;
import org.junit.jupiter.api.Test;

class PerformObservationStateTest {
    private static final RgbLightState GREEN = new RgbLightState(0, 96, 0, true);

    @Test
    void boundsMissingValuesAndKeepsAddressSpacesExplicit() {
        final PerformObservationState state = new PerformObservationState(16, 16, 8);
        state.setTrackName(2, "Bass");
        state.setTrackColor(2, GREEN);
        state.setSelectedVisibleTrack(2, true);
        state.selectSlot(18, 7);

        assertEquals("Bass", state.trackName(2));
        assertEquals(GREEN, state.trackColor(2));
        assertTrue(state.isSelectedVisibleTrack(2));
        assertEquals(18, state.selectedAbsoluteTrackIndex());
        assertEquals(7, state.selectedAbsoluteSceneIndex());
        assertEquals("", state.trackName(-1));
        assertFalse(state.isSelectedVisibleTrack(99));
    }

    @Test
    void meterUpdatesTrackCurrentAndPeakHoldWithoutCrossingBounds() {
        final PerformObservationState state = new PerformObservationState(16, 16, 8);
        state.updateRms(3, 42);
        state.updatePeak(3, 91);
        state.updatePeak(99, 127);

        assertEquals(42, state.rms(3));
        assertEquals(91, state.peak(3));
        assertEquals(91, state.peakHold(3));
        assertEquals(0, state.peak(99));
    }
}
