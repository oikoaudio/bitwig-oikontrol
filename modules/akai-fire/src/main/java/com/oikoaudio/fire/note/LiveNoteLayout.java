package com.oikoaudio.fire.note;

interface LiveNoteLayout {
    int[] notesForPad(int padIndex);

    NoteGridLayout.PadRole roleForPad(int padIndex);

    default int primaryNoteForPad(final int padIndex) {
        final int[] notes = notesForPad(padIndex);
        return notes.length == 0 ? -1 : notes[0];
    }
}
