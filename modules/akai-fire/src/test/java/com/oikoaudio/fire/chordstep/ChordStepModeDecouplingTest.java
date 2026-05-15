package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.note.PitchedSurfaceLayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ChordStepModeDecouplingTest {
    @Test
    void chordStepModeDoesNotUsePitchedSurfaceInheritance() {
        assertFalse(PitchedSurfaceLayer.class.isAssignableFrom(ChordStepMode.class));
        for (final Class<?> nestedClass : ChordStepMode.class.getDeclaredClasses()) {
            assertFalse(PitchedSurfaceLayer.class.isAssignableFrom(nestedClass));
        }
    }
}
