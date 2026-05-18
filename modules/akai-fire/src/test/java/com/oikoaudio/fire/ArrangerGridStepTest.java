package com.oikoaudio.fire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrangerGridStepTest {
    @Test
    void usesPhraseStepsForWideArrangerZoomLevels() {
        assertEquals(32.0, ArrangerGridStep.fromContentPerPixel(1.0 / 8.0, 4, 4));
        assertEquals(16.0, ArrangerGridStep.fromContentPerPixel(1.0 / 12.0, 4, 4));
    }

    @Test
    void usesBarStepsBeforeBeatStepsWhenEditingAtMediumWideZoomLevels() {
        assertEquals(4.0, ArrangerGridStep.fromContentPerPixel(1.0 / 20.0, 4, 4));
    }

    @Test
    void phraseStepsFollowCurrentTimeSignature() {
        assertEquals(24.0, ArrangerGridStep.fromContentPerPixel(1.0 / 8.0, 3, 4));
        assertEquals(12.0, ArrangerGridStep.fromContentPerPixel(1.0 / 8.0, 6, 16));
        assertEquals(1.5, ArrangerGridStep.fromContentPerPixel(1.0 / 20.0, 6, 16));
    }

    @Test
    void keepsBeatAndFineStepsForCloserZoomLevels() {
        assertEquals(1.0, ArrangerGridStep.fromContentPerPixel(1.0 / 100.0, 4, 4));
        assertEquals(0.25, ArrangerGridStep.fromContentPerPixel(1.0 / 400.0, 4, 4));
    }

    @Test
    void fallsBackToMeterBeatUnitWhenZoomIsUnavailable() {
        assertEquals(1.0, ArrangerGridStep.fromContentPerPixel(0.0, 4, 4));
        assertEquals(0.5, ArrangerGridStep.fromContentPerPixel(-1.0, 4, 8));
    }
}
