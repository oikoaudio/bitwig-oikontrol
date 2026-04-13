package com.oikoaudio.fire.melodic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MelodicPatternTest {

    @Test
    void defaultRecurrenceMapsToBitwigSafeValues() {
        final MelodicPattern.Step step = new MelodicPattern.Step(0, true, false, 48, 96,
                0.8, false, false);

        assertEquals(0, step.recurrenceLength());
        assertEquals(0, step.recurrenceMask());
        assertEquals(1, step.bitwigRecurrenceLength());
        assertEquals(1, step.bitwigRecurrenceMask());
    }

    @Test
    void fullMaskCollapsesBackToDefaultRecurrence() {
        final MelodicPattern.Step step = new MelodicPattern.Step(0, true, false, 48, 96,
                0.8, false, false).withRecurrence(4, 0b1111);

        assertEquals(0, step.recurrenceLength());
        assertEquals(0, step.recurrenceMask());
        assertEquals(1, step.bitwigRecurrenceLength());
        assertEquals(1, step.bitwigRecurrenceMask());
    }
}
