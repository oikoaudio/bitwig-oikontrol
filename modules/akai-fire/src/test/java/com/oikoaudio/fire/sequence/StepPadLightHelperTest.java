package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.RgbLigthState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void hidesClipStartIndicatorWhenPlayStartIsAtLoopStart() {
        assertEquals(-1, StepPadLightHelper.nearestColumnForShiftedClipStart(0.0, 8.0, 16));
        assertEquals(-1, StepPadLightHelper.nearestColumnForShiftedClipStart(8.0, 8.0, 16));
    }

    @Test
    void mapsShiftedClipStartToNearestVisibleColumn() {
        assertEquals(4, StepPadLightHelper.nearestColumnForShiftedClipStart(2.0, 8.0, 16));
        assertEquals(5, StepPadLightHelper.nearestColumnForShiftedClipStart(2.6, 8.0, 16));
    }

    @Test
    void mapsShiftedClipStartToNearestColumnOnVisibleStepPage() {
        assertEquals(3, StepPadLightHelper.nearestVisibleColumnForShiftedClipStart(
                8.75, 16.0, 0.25, 32, 32, 16));
        assertEquals(-1, StepPadLightHelper.nearestVisibleColumnForShiftedClipStart(
                2.0, 16.0, 0.25, 32, 32, 16));
    }

    @Test
    void overlaysClipStartHueOnlyOnMatchingColumn() {
        assertEquals(new RgbLigthState(30, 0, 127, true),
                StepPadLightHelper.renderClipStartColumnOverlay(3, 3, RgbLigthState.GRAY_1));
        assertEquals(RgbLigthState.GRAY_1,
                StepPadLightHelper.renderClipStartColumnOverlay(2, 3, RgbLigthState.GRAY_1));
    }
}
