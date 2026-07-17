package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DrumSequenceModeVariationTest {

    @Test
    void convertsTheWholeLoopToFineObservationStepsWithoutBoundaryRounding() {
        assertEquals(0, DrumSequenceMode.fineStepsForLoopLength(0.0));
        assertEquals(1, DrumSequenceMode.fineStepsForLoopLength(1.0 / 64.0));
        assertEquals(512, DrumSequenceMode.fineStepsForLoopLength(8.0));
        assertEquals(513, DrumSequenceMode.fineStepsForLoopLength(8.0 + 1.0 / 64.0));
    }
}
