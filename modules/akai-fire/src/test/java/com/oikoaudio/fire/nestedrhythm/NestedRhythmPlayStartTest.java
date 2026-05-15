package com.oikoaudio.fire.nestedrhythm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NestedRhythmPlayStartTest {

    @Test
    void beatStepFollowsTheMeterDenominator() {
        assertEquals(1.0, NestedRhythmPlayStart.beatStep(4));
        assertEquals(0.5, NestedRhythmPlayStart.beatStep(8));
    }

    @Test
    void incrementWrapsWithinTheCurrentLoopLength() {
        assertEquals(0.0, NestedRhythmPlayStart.increment(3.0, 4.0, 4, 1));
        assertEquals(3.0, NestedRhythmPlayStart.increment(0.0, 4.0, 4, -1));
    }

    @Test
    void incrementUsesMeterAwareBeatUnits() {
        assertEquals(1.5, NestedRhythmPlayStart.increment(0.0, 2.0, 8, 3));
    }

    @Test
    void incrementByStepSupportsFineMovement() {
        assertEquals(0.125, NestedRhythmPlayStart.incrementByStep(0.0, 1.0, 0.125, 1));
        assertEquals(0.875, NestedRhythmPlayStart.incrementByStep(0.0, 1.0, 0.125, -1));
    }

    @Test
    void snapToGridUsesNearestCoarseStep() {
        assertEquals(1.0, NestedRhythmPlayStart.snapToGrid(1.02, 4.0, 1.0));
        assertEquals(2.0, NestedRhythmPlayStart.snapToGrid(1.52, 4.0, 1.0));
        assertEquals(0.0, NestedRhythmPlayStart.snapToGrid(3.9, 4.0, 1.0));
    }
}
