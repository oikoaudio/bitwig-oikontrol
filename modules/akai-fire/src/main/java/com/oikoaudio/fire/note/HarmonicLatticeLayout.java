package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScale;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class HarmonicLatticeLayout implements LiveNoteLayout {
    static final int[] GRID_SPANS = {7, 14, 21};
    static final int MIN_OCTAVE_REACH = 1;
    static final int MAX_OCTAVE_REACH = 3;

    private static final int PAD_COUNT = 64;
    private static final int PAD_COLUMNS = 16;
    private static final int PAD_ROWS = 4;
    private static final int BASS_COLUMNS = 2;
    private static final int[] ROW_FINE_PHASES = {0, 2, 4, 6};

    private final MusicalScale scale;
    private final int rootNote;
    private final int octave;
    private final int gridSpan;
    private final int octaveReach;
    private final boolean bassColumnsEnabled;
    private final int glissSteps;
    private final int scaleDegreeCount;

    HarmonicLatticeLayout(final MusicalScale scale, final int rootNote, final int octave,
                          final int gridSpan, final int octaveReach,
                          final boolean bassColumnsEnabled, final int glissSteps) {
        this.scale = scale;
        this.rootNote = rootNote;
        this.octave = octave;
        this.gridSpan = gridSpan;
        this.octaveReach = octaveReach;
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
        final int bassNote = clampMidi(resolveTertianSeriesMidi(bassIndex, octave));
        return bassNote < 0 ? new int[0] : new int[]{bassNote};
    }

    private int[] harmonicNotesForPad(final int harmonicColumn, final int rowFromBottom) {
        final int anchorSeriesIndex = harmonicColumn + glissSteps;
        final int rowPhase = ROW_FINE_PHASES[Math.max(0, Math.min(ROW_FINE_PHASES.length - 1, rowFromBottom))];
        final int rowBaseOctave = octave + rowFromBottom;
        final Set<Integer> candidates = new LinkedHashSet<>();
        for (int candidateIndex = 0; ; candidateIndex++) {
            final int fineIndex = rowPhase + candidateIndex * 2;
            if (fineIndex >= gridSpan) {
                break;
            }
            final int octaveLayer = fineIndex / 7;
            if (octaveLayer >= octaveReach) {
                break;
            }
            final int midiNote = clampMidi(resolveTertianSeriesMidi(anchorSeriesIndex + candidateIndex,
                    rowBaseOctave + octaveLayer));
            if (midiNote >= 0) {
                candidates.add(midiNote);
            }
        }
        return candidates.stream().mapToInt(Integer::intValue).toArray();
    }

    private int resolveTertianSeriesMidi(final int seriesIndex, final int baseOctave) {
        if (scaleDegreeCount <= 0) {
            return -1;
        }
        final int normalizedSeries = Math.max(0, seriesIndex);
        final int degreeTravel = normalizedSeries * 2;
        final int octaveFromWrap = Math.floorDiv(degreeTravel, scaleDegreeCount);
        final int degreeIndex = Math.floorMod(degreeTravel, scaleDegreeCount);
        return scale.computeNote(rootNote, baseOctave + 1 + octaveFromWrap, degreeIndex);
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
}
