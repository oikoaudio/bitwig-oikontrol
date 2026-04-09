package com.oikoaudio.fire.melodic;

import com.bitwig.extensions.framework.MusicalScale;

import java.util.ArrayList;
import java.util.List;

public record MelodicPhraseContext(MusicalScale scale, int rootNote, int baseMidiNote) {
    public int pitchForDegree(final int octaveOffset, final int degree) {
        final int octave = Math.max(0, Math.min(8, baseMidiNote / 12 + 1 + octaveOffset));
        return Math.max(0, Math.min(127, scale.computeNote(rootNote, octave, degree)));
    }

    public List<Integer> collapsedScaleNotes(final int count) {
        final List<Integer> notes = new ArrayList<>(count);
        final int start = Math.max(0, baseMidiNote - 12);
        int candidate = start;
        while (notes.size() < count && candidate <= 127) {
            if (scale.isMidiNoteInScale(rootNote, candidate)) {
                notes.add(candidate);
            }
            candidate++;
        }
        while (notes.size() < count) {
            notes.add(127);
        }
        return notes;
    }

    public List<Integer> collapsedScaleRange(final int count) {
        final List<Integer> notes = new ArrayList<>(count);
        final int start = Math.max(0, baseMidiNote - 17);
        int candidate = start;
        while (notes.size() < count && candidate <= 127) {
            if (scale.isMidiNoteInScale(rootNote, candidate)) {
                notes.add(candidate);
            }
            candidate++;
        }
        return notes;
    }
}
