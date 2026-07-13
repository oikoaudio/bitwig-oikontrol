package com.oikoaudio.fire.fugue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oikoaudio.fire.sequence.NoteVariationParameter;
import org.junit.jupiter.api.Test;

class FugueStepModeVariationTest {

    @Test
    void mapsOnlyTheExistingContinuousNoteValueEncoders() {
        assertEquals(
                NoteVariationParameter.VELOCITY,
                FugueStepMode.noteVariationParameter(0).orElseThrow());
        assertEquals(
                NoteVariationParameter.CHANCE,
                FugueStepMode.noteVariationParameter(1).orElseThrow());
        assertTrue(FugueStepMode.noteVariationParameter(2).isEmpty());
        assertTrue(FugueStepMode.noteVariationParameter(3).isEmpty());
    }

    @Test
    void resetsToFuguesBaseInsertionValues() {
        assertEquals(
                96.0 / 127.0, FugueStepMode.noteVariationDefault(NoteVariationParameter.VELOCITY));
        assertEquals(1.0, FugueStepMode.noteVariationDefault(NoteVariationParameter.CHANCE));
    }
}
