package com.oikoaudio.fire.values;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClipLoopWindowTest {

    @Test
    void convertsAbsolutePositionsToAMovedLoopWindow() {
        assertEquals(32, ClipLoopWindow.startStep(8.0, 0.25));
        assertEquals(3, ClipLoopWindow.relativeStep(35, 8.0, 0.25));
        assertEquals(2.0, ClipLoopWindow.relativeBeat(10.0, 8.0, 8.0), 0.0001);
        assertEquals(7.0, ClipLoopWindow.relativeBeat(7.0, 8.0, 8.0), 0.0001);
        assertEquals(10.0, ClipLoopWindow.absoluteBeat(2.0, 8.0, 8.0), 0.0001);
    }
}
