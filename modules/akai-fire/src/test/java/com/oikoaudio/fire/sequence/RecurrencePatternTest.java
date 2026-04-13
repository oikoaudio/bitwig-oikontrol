package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrencePatternTest {

    @Test
    void defaultPatternMapsToSafeBitwigValues() {
        final RecurrencePattern pattern = RecurrencePattern.of(0, 0);

        assertTrue(pattern.isDefault());
        assertEquals(1, pattern.bitwigLength());
        assertEquals(1, pattern.bitwigMask());
        assertEquals(8, pattern.effectiveSpan());
        assertEquals(0b11111111, pattern.effectiveMask(8));
        assertEquals("Off", pattern.summary());
    }

    @Test
    void spanGestureEntersRecurrenceFromDefaultState() {
        final RecurrencePattern pattern = RecurrencePattern.of(0, 0).applySpanGesture(4);

        assertEquals(4, pattern.length());
        assertEquals(0b0001, pattern.mask());
        assertEquals("4:0001", pattern.summary());
    }

    @Test
    void repeatingCurrentSpanGestureClearsRecurrence() {
        final RecurrencePattern pattern = RecurrencePattern.of(4, 0b0101).applySpanGesture(4);

        assertTrue(pattern.isDefault());
        assertEquals("Off", pattern.summary());
    }

    @Test
    void togglingMaskNeverLeavesRecurrenceEmpty() {
        final RecurrencePattern pattern = RecurrencePattern.of(4, 0b0001).toggledAt(0);

        assertEquals(4, pattern.length());
        assertEquals(0b0001, pattern.mask());
    }
}
