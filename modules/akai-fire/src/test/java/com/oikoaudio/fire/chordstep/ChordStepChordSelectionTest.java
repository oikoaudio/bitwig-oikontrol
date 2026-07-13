package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bitwig.extensions.framework.MusicalScale;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChordStepChordSelectionTest {
    private static final MusicalScale MAJOR =
            new MusicalScale("Major", new int[] {0, 2, 4, 5, 7, 9, 11});

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
        assertEquals("Audible 1/2", selection.familyDisplayLabel());
    }

    @Test
    void rendersBuilderNotesSorted() {
        final ChordStepChordSelection selection = new ChordStepChordSelection();
        selection.replaceBuilderNotes(List.of(72, 60, 67));

        assertArrayEquals(new int[] {60, 67, 72}, selection.renderSelectedChord(MAJOR, 0));
        assertEquals("C G C", selection.chordName());
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

    @Test
    void rendersTheUnionOfSimultaneouslySelectedPresetSlots() {
        final ChordStepChordSelection selection = new ChordStepChordSelection();
        selection.adjustFamily(1);
        selection.selectSlot(0);
        final int[] first = selection.renderSelectedChord(MAJOR, 0);
        selection.selectSlot(2);
        final int[] second = selection.renderSelectedChord(MAJOR, 0);

        selection.selectSlots(Set.of(0, 2), 2);

        final int[] expected =
                java.util.stream.IntStream.concat(
                                java.util.Arrays.stream(first), java.util.Arrays.stream(second))
                        .distinct()
                        .sorted()
                        .toArray();
        assertArrayEquals(expected, selection.renderSelectedChord(MAJOR, 0));
        assertTrue(selection.isSlotSelected(0));
        assertTrue(selection.isSlotSelected(2));
        assertEquals("2 Chords", selection.chordName());
    }
}
