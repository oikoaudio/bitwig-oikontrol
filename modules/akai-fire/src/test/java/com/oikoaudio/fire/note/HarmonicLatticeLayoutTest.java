package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarmonicLatticeLayoutTest {

    @Test
    void rowsExposeDifferentAnchorNotesForSameHarmonicColumn() {
        final HarmonicLatticeLayout layout = new HarmonicLatticeLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                2,
                7,
                1,
                true,
                0);

        final int topRowPad = 2;
        final int nextRowPad = 18;
        final int thirdRowPad = 34;
        final int bottomRowPad = 50;

        assertNotEquals(layout.primaryNoteForPad(topRowPad), layout.primaryNoteForPad(nextRowPad));
        assertNotEquals(layout.primaryNoteForPad(nextRowPad), layout.primaryNoteForPad(thirdRowPad));
        assertNotEquals(layout.primaryNoteForPad(thirdRowPad), layout.primaryNoteForPad(bottomRowPad));
    }

    @Test
    void bassColumnsRemainSingleNotesWhileHarmonicPadsProduceCandidateLists() {
        final HarmonicLatticeLayout layout = new HarmonicLatticeLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                2,
                14,
                2,
                true,
                0);

        assertEquals(1, layout.notesForPad(0).length);
        assertTrue(layout.notesForPad(2).length > 1);
    }
}
