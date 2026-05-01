package com.oikoaudio.fire.note;

/**
 * Fire matrix layout for playing the currently visible Bitwig Drum Machine pad window.
 *
 * Pads run left-to-right from the bottom row, so the bottom-left Fire pad triggers the
 * first visible Drum Machine pad and the top-right triggers the 64th visible pad.
 */
final class DrumMachinePadLayout implements LiveNoteLayout {
    static final int PAD_WINDOW_SIZE = NoteGridLayout.PAD_COUNT;
    private static final int SELECTOR_COLUMNS = 4;
    private static final int BONGO_LEFT_START_COLUMN = 5;
    private static final int BONGO_RIGHT_START_COLUMN = 11;

    enum Layout {
        GRID64("Grid64"),
        VELOCITY("Velocity"),
        BONGOS("Bongos");

        private final String displayName;

        Layout(final String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }

    }

    private final int firstVisibleMidiNote;
    private final Layout layout;
    private final int primaryPadOffset;
    private final int secondaryPadOffset;

    DrumMachinePadLayout(final int firstVisibleMidiNote) {
        this(firstVisibleMidiNote, Layout.GRID64, 0);
    }

    DrumMachinePadLayout(final int firstVisibleMidiNote, final Layout layout, final int selectedPadOffset) {
        this(firstVisibleMidiNote, layout, selectedPadOffset, Math.min(PAD_WINDOW_SIZE - 1, selectedPadOffset + 1));
    }

    DrumMachinePadLayout(final int firstVisibleMidiNote, final Layout layout, final int primaryPadOffset,
                         final int secondaryPadOffset) {
        this.firstVisibleMidiNote = firstVisibleMidiNote;
        this.layout = layout;
        this.primaryPadOffset = clampOffset(primaryPadOffset);
        this.secondaryPadOffset = clampOffset(secondaryPadOffset);
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
        final int noteOffset = playableOffsetForPad(padIndex);
        if (noteOffset < 0) {
            return -1;
        }
        final int midiNote = firstVisibleMidiNote + noteOffset;
        return midiNote <= 127 ? midiNote : -1;
    }

    int padBankIndexForPad(final int padIndex) {
        final int selectorOffset = selectorOffsetForPad(padIndex);
        if (selectorOffset >= 0) {
            return selectorOffset;
        }
        final int playableOffset = playableOffsetForPad(padIndex);
        return playableOffset >= 0 && playableOffset < PAD_WINDOW_SIZE ? playableOffset : -1;
    }

    int selectorOffsetForPad(final int padIndex) {
        if (!usesSelectorBlock() || !isSelectorPad(padIndex)) {
            return -1;
        }
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        final int rowFromBottom = NoteGridLayout.PAD_ROWS - 1 - (padIndex / NoteGridLayout.PAD_COLUMNS);
        return column + rowFromBottom * SELECTOR_COLUMNS;
    }

    Layout layout() {
        return layout;
    }

    private int playableOffsetForPad(final int padIndex) {
        if (padIndex < 0 || padIndex >= NoteGridLayout.PAD_COUNT) {
            return -1;
        }
        if (layout == Layout.GRID64) {
            return padWindowIndexForPad(padIndex);
        }
        final int selectorOffset = selectorOffsetForPad(padIndex);
        if (selectorOffset >= 0) {
            return selectorOffset;
        }
        if (layout == Layout.BONGOS) {
            final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
            if (column < BONGO_LEFT_START_COLUMN || column == BONGO_RIGHT_START_COLUMN - 1) {
                return -1;
            }
            return column >= BONGO_RIGHT_START_COLUMN ? secondaryPadOffset : primaryPadOffset;
        }
        return primaryPadOffset;
    }

    private boolean usesSelectorBlock() {
        return layout != Layout.GRID64;
    }

    private static boolean isSelectorPad(final int padIndex) {
        return padIndex >= 0
                && padIndex < NoteGridLayout.PAD_COUNT
                && (padIndex % NoteGridLayout.PAD_COLUMNS) < SELECTOR_COLUMNS;
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

    private static int clampOffset(final int offset) {
        return Math.max(0, Math.min(PAD_WINDOW_SIZE - 1, offset));
    }
}
