package com.oikoaudio.fire.nestedrhythm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NestedRhythmLoopLengthTest {

    @Test
    void firstStepKeepsMinimumPositiveLoopLength() {
        assertEquals(53, NestedRhythmLoopLength.loopFineSteps(1680, 0));
    }

    @Test
    void halfWayStepUsesHalfOfThePhraseLength() {
        assertEquals(840, NestedRhythmLoopLength.loopFineSteps(1680, 15));
        assertEquals(2.0, NestedRhythmLoopLength.loopLengthBeats(4.0, 15));
    }

    @Test
    void finalStepReachesTheFullPhraseLength() {
        assertEquals(1680, NestedRhythmLoopLength.loopFineSteps(1680, 31));
        assertEquals(4.0, NestedRhythmLoopLength.loopLengthBeats(4.0, 31));
    }

    @Test
    void settingsFromBeatsUsesSmallestContainingBarCount() {
        final NestedRhythmLoopLength.Settings settings = NestedRhythmLoopLength.settingsFromBeats(
                6.0, 4.0, new int[]{1, 2, 4});

        assertEquals(2, settings.barCount());
        assertEquals(23, settings.lastStepIndex());
    }

    @Test
    void settingsFromBeatsCanRepresentOddPartialLengthsInLargerContainers() {
        final NestedRhythmLoopLength.Settings settings = NestedRhythmLoopLength.settingsFromBeats(
                12.0, 4.0, new int[]{1, 2, 4});

        assertEquals(4, settings.barCount());
        assertEquals(23, settings.lastStepIndex());
    }

    @Test
    void settingsFromBeatsUsesMeterLengthAsOneBar() {
        final NestedRhythmLoopLength.Settings settings = NestedRhythmLoopLength.settingsFromBeats(
                5.0, 5.0, new int[]{1, 2, 4});

        assertEquals(1, settings.barCount());
        assertEquals(31, settings.lastStepIndex());
    }

    @Test
    void lastStepIndexIsClampedIntoVisibleRange() {
        assertEquals(0, NestedRhythmLoopLength.normalizeLastStepIndex(-1));
        assertEquals(31, NestedRhythmLoopLength.normalizeLastStepIndex(99));
    }
}
