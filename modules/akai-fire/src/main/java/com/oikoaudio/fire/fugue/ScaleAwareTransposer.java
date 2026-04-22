package com.oikoaudio.fire.fugue;

import com.bitwig.extensions.framework.MusicalScale;

import java.util.ArrayList;
import java.util.List;

public final class ScaleAwareTransposer {
    private ScaleAwareTransposer() {
    }

    public static int transpose(final int midiNote, final int semitoneOffset,
                                final MusicalScale scale, final int rootNote) {
        final int target = clampMidi(midiNote + semitoneOffset);
        if (Math.floorMod(semitoneOffset, 12) == 0) {
            return target;
        }
        if (scale == null || scale.isMidiNoteInScale(rootNote, target)) {
            return target;
        }
        for (int distance = 1; distance <= 12; distance++) {
            final int upward = target + distance;
            if (upward <= 127 && scale.isMidiNoteInScale(rootNote, upward)) {
                return upward;
            }
            final int downward = target - distance;
            if (downward >= 0 && scale.isMidiNoteInScale(rootNote, downward)) {
                return downward;
            }
        }
        return target;
    }

    public static int transposeByScaleDegrees(final int midiNote, final int degreeOffset,
                                              final MusicalScale scale, final int rootNote) {
        if (degreeOffset == 0 || scale == null) {
            return clampMidi(midiNote);
        }
        final List<Integer> scaleNotes = scaleNotes(scale, rootNote);
        if (scaleNotes.isEmpty()) {
            return clampMidi(midiNote);
        }
        final int sourceIndex = nearestScaleIndex(scaleNotes, midiNote);
        final int targetIndex = Math.max(0, Math.min(scaleNotes.size() - 1, sourceIndex + degreeOffset));
        return scaleNotes.get(targetIndex);
    }

    public static int transposeDiatonicThenChromatic(final int midiNote, final int degreeOffset,
                                                     final int semitoneOffset,
                                                     final MusicalScale scale, final int rootNote) {
        final int degreeTarget = transposeByScaleDegrees(midiNote, degreeOffset, scale, rootNote);
        return transpose(degreeTarget, semitoneOffset, scale, rootNote);
    }

    private static List<Integer> scaleNotes(final MusicalScale scale, final int rootNote) {
        final List<Integer> notes = new ArrayList<>();
        for (int note = 0; note <= 127; note++) {
            if (scale.isMidiNoteInScale(rootNote, note)) {
                notes.add(note);
            }
        }
        return notes;
    }

    private static int nearestScaleIndex(final List<Integer> scaleNotes, final int midiNote) {
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < scaleNotes.size(); i++) {
            final int distance = Math.abs(scaleNotes.get(i) - midiNote);
            if (distance < bestDistance) {
                bestIndex = i;
                bestDistance = distance;
            }
        }
        return bestIndex;
    }

    private static int clampMidi(final int note) {
        return Math.max(0, Math.min(127, note));
    }
}
