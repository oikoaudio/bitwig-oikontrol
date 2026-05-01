package com.oikoaudio.fire.note;

/**
 * Fire matrix layout for playing the currently visible Bitwig Drum Machine pad window.
 *
 * Pads run left-to-right from the bottom row, so the bottom-left Fire pad triggers the
 * first visible Drum Machine pad and the top-right triggers the 64th visible pad.
 */
final class DrumMachinePadLayout implements LiveNoteLayout {
    static final int PAD_WINDOW_SIZE = NoteGridLayout.PAD_COUNT;

    private final int firstVisibleMidiNote;

    DrumMachinePadLayout(final int firstVisibleMidiNote) {
        this.firstVisibleMidiNote = firstVisibleMidiNote;
    }

    @Override
    public int[] notesForPad(final int padIndex) {
        final int midiNote = noteForPad(padIndex);
        return midiNote < 0 ? new int[0] : new int[]{midiNote};
    }

    int noteForPad(final int padIndex) {
        if (padIndex < 0 || padIndex >= NoteGridLayout.PAD_COUNT) {
            return -1;
        }
        final int noteOffset = padWindowIndexForPad(padIndex);
        final int midiNote = firstVisibleMidiNote + noteOffset;
        return midiNote <= 127 ? midiNote : -1;
    }

    int padBankIndexForPad(final int padIndex) {
        final int noteOffset = padWindowIndexForPad(padIndex);
        return noteOffset >= 0 && noteOffset < PAD_WINDOW_SIZE ? noteOffset : -1;
    }

    @Override
    public NoteGridLayout.PadRole roleForPad(final int padIndex) {
        return noteForPad(padIndex) < 0 ? NoteGridLayout.PadRole.UNAVAILABLE : NoteGridLayout.PadRole.IN_SCALE;
    }

    private static int padWindowIndexForPad(final int padIndex) {
        if (padIndex < 0 || padIndex >= NoteGridLayout.PAD_COUNT) {
            return -1;
        }
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        final int rowFromBottom = NoteGridLayout.PAD_ROWS - 1 - (padIndex / NoteGridLayout.PAD_COLUMNS);
        return column + rowFromBottom * NoteGridLayout.PAD_COLUMNS;
    }
}
