package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScale;

final class HarmonicLatticeLayout implements LiveNoteLayout {
    static final int[] NOTE_COUNTS = {1, 2, 3};

    private static final int PAD_COUNT = 64;
    private static final int PAD_COLUMNS = 16;
    private static final int PAD_ROWS = 4;
    private static final int BASS_COLUMNS = 2;

    private final MusicalScale scale;
    private final int rootNote;
    private final int octave;
    private final int noteCount;
    private final int octaveSpan;
    private final boolean bassColumnsEnabled;
    private final int glissSteps;
    private final int scaleDegreeCount;

    HarmonicLatticeLayout(final MusicalScale scale, final int rootNote, final int octave,
                          final int noteCount, final int octaveSpan,
                          final boolean bassColumnsEnabled, final int glissSteps) {
        this.scale = scale;
        this.rootNote = rootNote;
        this.octave = octave;
        this.noteCount = noteCount;
        this.octaveSpan = octaveSpan;
        this.bassColumnsEnabled = bassColumnsEnabled;
        this.glissSteps = glissSteps;
        this.scaleDegreeCount = detectScaleDegreeCount(scale, rootNote);
    }

    @Override
    public int[] notesForPad(final int padIndex) {
        if (padIndex < 0 || padIndex >= PAD_COUNT) {
            return new int[0];
        }
        final int column = padIndex % PAD_COLUMNS;
        final int rowFromBottom = PAD_ROWS - 1 - (padIndex / PAD_COLUMNS);
        if (bassColumnsEnabled && column < BASS_COLUMNS) {
            return bassNotesForPad(column, rowFromBottom);
        }
        final int harmonicColumn = bassColumnsEnabled ? column - BASS_COLUMNS : column;
        return harmonicNotesForPad(harmonicColumn, rowFromBottom);
    }

    @Override
    public NoteGridLayout.PadRole roleForPad(final int padIndex) {
        final int midiNote = primaryNoteForPad(padIndex);
        if (midiNote < 0) {
            return NoteGridLayout.PadRole.UNAVAILABLE;
        }
        if (scale.isRootMidiNote(rootNote, midiNote)) {
            return NoteGridLayout.PadRole.ROOT;
        }
        if (scale.isMidiNoteInScale(rootNote, midiNote)) {
            return NoteGridLayout.PadRole.IN_SCALE;
        }
        return NoteGridLayout.PadRole.OUT_OF_SCALE;
    }

    private int[] bassNotesForPad(final int column, final int rowFromBottom) {
        final int bassIndex = rowFromBottom * BASS_COLUMNS + column;
        final int bassNote = clampMidi(resolveRegisteredRunMidi(bassIndex, octave));
        return bassNote < 0 ? new int[0] : new int[]{bassNote};
    }

    private int[] harmonicNotesForPad(final int harmonicColumn, final int rowFromBottom) {
        final int startIndex = harmonicColumn - rowFromBottom * 2 + glissSteps;
        final int rowBaseOctave = octave + rowFromBottom;
        final int[] notes = new int[noteCount * octaveSpan];
        int out = 0;
        for (int octaveOffset = 0; octaveOffset < octaveSpan; octaveOffset++) {
            for (int i = 0; i < noteCount; i++) {
                final int baseNote = clampMidi(resolveRegisteredRunMidi(startIndex + i, rowBaseOctave));
                notes[out++] = baseNote < 0 ? -1 : wrapMidi(baseNote + octaveOffset * 12);
            }
        }
        return notes;
    }

    private int resolveRegisteredRunMidi(final int seriesIndex, final int baseOctave) {
        if (scaleDegreeCount <= 0) {
            return -1;
        }
        final int degreeTravel = seriesIndex * 2;
        final int octaveFromWrap = Math.floorDiv(degreeTravel, scaleDegreeCount);
        final int degreeIndex = Math.floorMod(degreeTravel, scaleDegreeCount);
        final int compactOffset = Math.floorMod(seriesIndex, 2) == 0 ? 0 : -1;
        return scale.computeNote(rootNote, baseOctave + 1 + octaveFromWrap + compactOffset, degreeIndex);
    }

    private static int detectScaleDegreeCount(final MusicalScale scale, final int rootNote) {
        int count = 0;
        for (int semitone = 0; semitone < 12; semitone++) {
            if (scale.isMidiNoteInScale(rootNote, rootNote + semitone)) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static int clampMidi(final int midiNote) {
        return midiNote >= 0 && midiNote <= 127 ? midiNote : -1;
    }

    private static int wrapMidi(final int midiNote) {
        int wrapped = midiNote;
        while (wrapped > 127) {
            wrapped -= 12;
        }
        while (wrapped < 0) {
            wrapped += 12;
        }
        return wrapped;
    }
}
