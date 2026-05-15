package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LivePadSurfaceLayerPitchGlissDisplayTest {

    @Test
    void fifthOctaveDisplayUsesSemitoneOffsetsInHarmonicMode() {
        assertEquals(5, LivePadSurfaceLayer.displayPitchGlissValue(true, 5, 0));
        assertEquals(12, LivePadSurfaceLayer.displayPitchGlissValue(true, 12, 0));
        assertEquals(19, LivePadSurfaceLayer.displayPitchGlissValue(true, 19, 0));
        assertEquals(24, LivePadSurfaceLayer.displayPitchGlissValue(true, 24, 0));
    }

    @Test
    void scaleDegreeDisplayStillUsesScaleDegreeOffset() {
        assertEquals(3, LivePadSurfaceLayer.displayPitchGlissValue(false, 19, 3));
    }
}
