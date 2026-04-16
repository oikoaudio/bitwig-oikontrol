package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteGridLayoutTest {

    @Test
    void mapsChromaticPadsAcrossRowsInFourths() {
        final NoteGridLayout layout = new NoteGridLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                3,
                false);

        assertEquals(63, layout.noteForPad(0));
        assertEquals(48, layout.noteForPad(48));
        assertEquals(63, layout.noteForPad(63));
    }

    @Test
    void mapsInKeyPadsByScaleDegree() {
        final NoteGridLayout layout = new NoteGridLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                3,
                true);

        assertEquals(74, layout.noteForPad(0));
        assertEquals(48, layout.noteForPad(48));
        assertEquals(74, layout.noteForPad(63));
    }

    @Test
    void classifiesRootScaleAndOutsideNotes() {
        final NoteGridLayout layout = new NoteGridLayout(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                3,
                false);

        assertEquals(NoteGridLayout.PadRole.ROOT, layout.roleForPad(48));
        assertEquals(NoteGridLayout.PadRole.OUT_OF_SCALE, layout.roleForPad(49));
        assertEquals(NoteGridLayout.PadRole.IN_SCALE, layout.roleForPad(50));
    }
}
