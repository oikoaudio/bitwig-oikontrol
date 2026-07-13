package com.personal.chords;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChordDetectorTest {
    @Test
    void midiNoteNamesUseBitwigOctaveLabels() {
        assertEquals("C2", ChordDetector.midiNoteName(48));
        assertEquals("C3", ChordDetector.midiNoteName(60));
    }

    @Test
    void detectedChordNotesUseBitwigOctaveLabels() {
        final ChordDetector.ChordResult result = new ChordDetector().detect(List.of(48, 52, 55));

        assertEquals(List.of("C2", "E2", "G2"), result.midiNoteNames());
    }
}
