package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MulticlipXoxLayoutTest {
    @Test
    void mapsTheFourRowsToScenesLanesAndOneThirtyTwoStepPattern() {
        assertTrue(MulticlipXoxLayout.isScenePad(0));
        assertEquals(15, MulticlipXoxLayout.sceneInPage(15));
        assertTrue(MulticlipXoxLayout.isLanePad(16));
        assertEquals(15, MulticlipXoxLayout.childPosition(31));
        assertTrue(MulticlipXoxLayout.isPatternPad(32));
        assertEquals(0, MulticlipXoxLayout.visibleStep(32));
        assertEquals(31, MulticlipXoxLayout.visibleStep(63));
        assertFalse(MulticlipXoxLayout.isPatternPad(31));
    }
}
