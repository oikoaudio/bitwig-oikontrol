package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StepPadLightHelperTest {

    @Test
    void detectsStepsWithinVisibleLoopRange() {
        assertTrue(StepPadLightHelper.isStepWithinVisibleLoop(0, 16));
        assertTrue(StepPadLightHelper.isStepWithinVisibleLoop(15, 16));
        assertFalse(StepPadLightHelper.isStepWithinVisibleLoop(16, 16));
        assertFalse(StepPadLightHelper.isStepWithinVisibleLoop(-1, 16));
    }
}
