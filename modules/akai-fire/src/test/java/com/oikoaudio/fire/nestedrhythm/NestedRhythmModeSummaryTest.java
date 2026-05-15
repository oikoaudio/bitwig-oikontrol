package com.oikoaudio.fire.nestedrhythm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NestedRhythmModeSummaryTest {
    @Test
    void defaultSummaryUsesNeutralBaseLabel() {
        assertEquals("Base/4/4",
                NestedRhythmMode.summaryLabel(0, 3, 0, 2, 0.0, "4/4", 1));
    }

    @Test
    void summaryShowsOnlyActiveGenerationModifiers() {
        assertEquals("T2x3/R1x2/C15%/4/4x2",
                NestedRhythmMode.summaryLabel(2, 3, 1, 2, 0.15, "4/4", 2));
    }

    @Test
    void oneBarCountIsOnlyShownWhenGreaterThanOne() {
        assertEquals("Base/4/4x2",
                NestedRhythmMode.summaryLabel(0, 3, 0, 2, 0.0, "4/4", 2));
    }
}
