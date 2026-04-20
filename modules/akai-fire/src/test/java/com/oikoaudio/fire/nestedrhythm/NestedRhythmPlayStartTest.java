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
}
