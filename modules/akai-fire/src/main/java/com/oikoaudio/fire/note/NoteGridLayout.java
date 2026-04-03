package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScale;

public final class NoteGridLayout {
    public static final int PAD_COUNT = 64;
    public static final int PAD_COLUMNS = 16;
    public static final int PAD_ROWS = 4;

    private static final int CHROMATIC_ROW_INTERVAL = 5;
    private static final int IN_KEY_ROW_INTERVAL = 3;

    public enum PadRole {
        ROOT,
        IN_SCALE,
        OUT_OF_SCALE,
        UNAVAILABLE
    }

    private final MusicalScale scale;
    private final int rootNote;
    private final int octave;
    private final boolean inKey;

    public NoteGridLayout(final MusicalScale scale, final int rootNote, final int octave, final boolean inKey) {
        this.scale = scale;
        this.rootNote = rootNote;
        this.octave = octave;
        this.inKey = inKey;
    }

    public int noteForPad(final int padIndex) {
        if (padIndex < 0 || padIndex >= PAD_COUNT) {
            return -1;
        }
        final int column = padIndex % PAD_COLUMNS;
        final int rowFromBottom = PAD_ROWS - 1 - (padIndex / PAD_COLUMNS);
        if (inKey) {
            return scale.computeNote(rootNote, octave + 1, column + rowFromBottom * IN_KEY_ROW_INTERVAL);
        }
        final int note = rootNote + (octave + 1) * 12 + column + rowFromBottom * CHROMATIC_ROW_INTERVAL;
        return note >= 0 && note <= 127 ? note : -1;
    }

    public PadRole roleForPad(final int padIndex) {
        final int midiNote = noteForPad(padIndex);
        if (midiNote < 0) {
            return PadRole.UNAVAILABLE;
        }
        if (scale.isRootMidiNote(rootNote, midiNote)) {
            return PadRole.ROOT;
        }
        if (scale.isMidiNoteInScale(rootNote, midiNote)) {
            return PadRole.IN_SCALE;
        }
        return PadRole.OUT_OF_SCALE;
    }

    public static String noteName(final int noteClass) {
        return switch (Math.floorMod(noteClass, 12)) {
            case 0 -> "C";
            case 1 -> "C#";
            case 2 -> "D";
            case 3 -> "D#";
            case 4 -> "E";
            case 5 -> "F";
            case 6 -> "F#";
            case 7 -> "G";
            case 8 -> "G#";
            case 9 -> "A";
            case 10 -> "A#";
            default -> "B";
        };
    }

    public static boolean isBlackKey(final int noteClass) {
        return switch (Math.floorMod(noteClass, 12)) {
            case 1, 3, 6, 8, 10 -> true;
            default -> false;
        };
    }
}
