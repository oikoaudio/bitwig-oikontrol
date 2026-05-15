package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ChordStepVisibleClipCache {
    private final int stepCount;
    private final Map<Integer, Set<Integer>> notesByStep = new HashMap<>();

    ChordStepVisibleClipCache(final int stepCount) {
        this.stepCount = stepCount;
    }

    void handleStepData(final int x, final int y, final int state) {
        if (x < 0 || x >= stepCount) {
            return;
        }
        final Set<Integer> stepNotes = notesByStep.computeIfAbsent(x, ignored -> new HashSet<>());
        if (state == NoteStep.State.Empty.ordinal()) {
            stepNotes.remove(y);
            if (stepNotes.isEmpty()) {
                notesByStep.remove(x);
            }
            return;
        }
        stepNotes.add(y);
    }

    boolean hasStepContent(final int stepIndex) {
        return notesByStep.containsKey(stepIndex);
    }

    Set<Integer> notesAtStep(final int stepIndex) {
        final Set<Integer> notes = notesByStep.get(stepIndex);
        if (notes == null || notes.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(notes);
    }
}
