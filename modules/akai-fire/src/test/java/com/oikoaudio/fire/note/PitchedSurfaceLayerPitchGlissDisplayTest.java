package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PitchedSurfaceLayerPitchGlissDisplayTest {

    @Test
    void fifthOctaveDisplayUsesSemitoneOffsetsInHarmonicMode() {
        assertEquals(5, PitchedSurfaceLayer.displayPitchGlissValue(true, 5, 0));
        assertEquals(12, PitchedSurfaceLayer.displayPitchGlissValue(true, 12, 0));
        assertEquals(19, PitchedSurfaceLayer.displayPitchGlissValue(true, 19, 0));
        assertEquals(24, PitchedSurfaceLayer.displayPitchGlissValue(true, 24, 0));
    }

    @Test
    void scaleDegreeDisplayStillUsesScaleDegreeOffset() {
        assertEquals(3, PitchedSurfaceLayer.displayPitchGlissValue(false, 19, 3));
    }
}
