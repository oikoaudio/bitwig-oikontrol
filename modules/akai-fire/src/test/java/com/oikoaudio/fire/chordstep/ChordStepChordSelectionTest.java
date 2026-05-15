package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.MusicalScale;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepChordSelectionTest {
    private static final MusicalScale MAJOR = new MusicalScale("Major", new int[] {0, 2, 4, 5, 7, 9, 11});

    @Test
    void startsInBuilderFamilyWithEmptyChord() {
        final ChordStepChordSelection selection = new ChordStepChordSelection();

        assertTrue(selection.isBuilderFamily());
        assertEquals("Builder", selection.familyLabel());
        assertEquals("Empty", selection.chordName());
        assertArrayEquals(new int[0], selection.renderSelectedChord(MAJOR, 0));
    }

    @Test
    void adjustsToPresetFamilyAndSelectsSlots() {
        final ChordStepChordSelection selection = new ChordStepChordSelection();

        assertTrue(selection.adjustFamily(1));
        assertFalse(selection.isBuilderFamily());
        assertTrue(selection.hasSlot(0));

        selection.selectSlot(2);

        assertEquals(2, selection.selectedSlot());
        assertEquals("Audible", selection.familyLabel());
        assertEquals("Minor", selection.chordName());
    }

    @Test
    void rendersBuilderNotesSorted() {
        final ChordStepChordSelection selection = new ChordStepChordSelection();
        selection.replaceBuilderNotes(List.of(72, 60, 67));

        assertArrayEquals(new int[] {60, 67, 72}, selection.renderSelectedChord(MAJOR, 0));
        assertEquals("3 notes C G C", selection.chordName());
    }

    @Test
    void tracksOctaveAndInterpretationDisplay() {
        final ChordStepChordSelection selection = new ChordStepChordSelection();

        assertTrue(selection.adjustOctave(1));
        assertTrue(selection.adjustInterpretation(1));

        assertEquals(1, selection.octaveOffset());
        assertEquals("InKey", selection.interpretationDisplayName());
        assertEquals("F1 InKey KC O+1", selection.interpretationSuffix(0));
    }
}
