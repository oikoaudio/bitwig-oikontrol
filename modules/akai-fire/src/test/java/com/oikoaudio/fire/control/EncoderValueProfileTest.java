package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncoderValueProfileTest {
    @Test
    void largeRangeUsesCoarseAndFineDeltas() {
        assertEquals(0.02, EncoderValueProfile.LARGE_RANGE.delta(false, 2), 0.0000001);
        assertEquals(0.005, EncoderValueProfile.LARGE_RANGE.delta(true, 2), 0.0000001);
    }

    @Test
    void compactRangeStaysSmallerThanLargeRange() {
        assertEquals(0.01, EncoderValueProfile.COMPACT_RANGE.delta(false, 2), 0.0000001);
        assertEquals(0.0025, EncoderValueProfile.COMPACT_RANGE.delta(true, 2), 0.0000001);
    }

    @Test
    void semitoneAndPitchProfilesStaySymmetricInFineMode() {
        assertEquals(EncoderValueProfile.SEMITONE_PARAMETER.delta(false, 1),
                EncoderValueProfile.SEMITONE_PARAMETER.delta(true, 1), 0.0000001);
        assertEquals(EncoderValueProfile.PITCH_PARAMETER.delta(false, 1),
                EncoderValueProfile.PITCH_PARAMETER.delta(true, 1), 0.0000001);
    }

    @Test
    void steppedValuesAdvanceByDiscreteIndex() {
        assertEquals(0.25, EncoderValueProfile.steppedValueAfterIncrement(0.0, 5, 1));
        assertEquals(0.5, EncoderValueProfile.steppedValueAfterIncrement(0.25, 5, 1));
    }

    @Test
    void steppedValuesClampAtRangeEdges() {
        assertEquals(0.0, EncoderValueProfile.steppedValueAfterIncrement(0.0, 5, -1));
        assertEquals(1.0, EncoderValueProfile.steppedValueAfterIncrement(1.0, 5, 1));
    }

    @Test
    void steppedValuesCanMoveMultipleIndexes() {
        assertEquals(0.75, EncoderValueProfile.steppedValueAfterIncrement(0.25, 5, 2));
        assertEquals(0.0, EncoderValueProfile.steppedValueAfterIncrement(0.75, 5, -3));
    }
}
