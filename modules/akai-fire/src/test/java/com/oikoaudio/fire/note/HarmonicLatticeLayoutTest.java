package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class HarmonicLatticeLayoutTest {

    @Test
    void cMajorRowsAreShiftedTwoStepsRight() {
        final HarmonicLatticeLayout layout = new HarmonicLatticeLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                2,
                1,
                1,
                true,
                0);

        assertEquals(36, layout.primaryNoteForPad(50)); // bottom row, first harmonic pad = C2
        assertEquals(48, layout.primaryNoteForPad(36)); // next row, shifted right by 2 => C3 at col 2
        assertEquals(60, layout.primaryNoteForPad(22)); // next row, shifted right by 4 => C4 at col 4
        assertEquals(72, layout.primaryNoteForPad(8));  // top row, shifted right by 6 => C5 at col 6
    }

    @Test
    void cMajorRootVoicingUsesCompactAlternatingOctaves() {
        final HarmonicLatticeLayout layout = new HarmonicLatticeLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                2,
                3,
                1,
                true,
                0);

        assertArrayEquals(new int[]{36, 28, 43}, layout.notesForPad(50)); // C2 E1 G2
        assertArrayEquals(new int[]{48, 40, 55}, layout.notesForPad(36)); // C3 E2 G3
    }

    @Test
    void bassColumnsRemainSingleNotes() {
        final HarmonicLatticeLayout layout = new HarmonicLatticeLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                2,
                3,
                1,
                true,
                0);

        assertEquals(1, layout.notesForPad(0).length);
    }

    @Test
    void harmonicPadsWrapAtRightEdgeWithoutReducingNoteCount() {
        final HarmonicLatticeLayout layout = new HarmonicLatticeLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                2,
                3,
                1,
                true,
                0);

        assertEquals(3, layout.notesForPad(63).length);
    }

    @Test
    void octaveSpanAddsOctavesAboveSelectedRunNotes() {
        final HarmonicLatticeLayout layout = new HarmonicLatticeLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                2,
                3,
                2,
                true,
                0);

        assertArrayEquals(new int[]{36, 28, 43, 48, 40, 55}, layout.notesForPad(50));
    }
}
