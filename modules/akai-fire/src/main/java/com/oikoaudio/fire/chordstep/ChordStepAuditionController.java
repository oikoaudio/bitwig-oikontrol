package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.values.Midi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class ChordStepAuditionController {
    private final NoteInput noteInput;
    private final Integer[] disabledTranslationTable = new Integer[128];
    private final Set<Integer> auditioningNotes = new HashSet<>();

    ChordStepAuditionController(final NoteInput noteInput) {
        this.noteInput = noteInput;
        Arrays.fill(disabledTranslationTable, -1);
    }

    void configureExpression() {
        noteInput.assignPolyphonicAftertouchToExpression(0, NoteInput.NoteExpression.TIMBRE_UP, 1);
    }

    void startAudition(final int[] notes, final int velocity) {
        stopAudition();
        for (final int midiNote : notes) {
            noteInput.sendRawMidiEvent(Midi.NOTE_ON, midiNote, velocity);
            auditioningNotes.add(midiNote);
        }
    }

    void stopAudition() {
        if (auditioningNotes.isEmpty()) {
            return;
        }
        for (final int midiNote : auditioningNotes) {
            noteInput.sendRawMidiEvent(Midi.NOTE_OFF, midiNote, 0);
        }
        auditioningNotes.clear();
    }

    void clearTranslation() {
        noteInput.setKeyTranslationTable(disabledTranslationTable);
    }
}
