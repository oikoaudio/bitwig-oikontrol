package com.oikoaudio.fire.melodic;

import com.bitwig.extension.controller.api.PinnableCursorClip;

import java.util.HashMap;
import java.util.Map;

final class MelodicStepClipWriter {
    private final Map<Integer, MelodicPattern.Step> pendingWrittenSteps = new HashMap<>();

    void writeToClip(final PinnableCursorClip clip, final MelodicPattern pattern, final double stepLength) {
        rememberPendingWrites(pattern);
        MelodicClipAdapter.writeToClip(clip, pattern, stepLength);
    }

    void rememberPendingWrite(final int stepIndex, final MelodicPattern.Step step) {
        pendingWrittenSteps.put(stepIndex, step);
    }

    MelodicPattern.Step pendingStepAt(final int stepIndex) {
        return pendingWrittenSteps.get(stepIndex);
    }

    void clearPendingWrite(final int stepIndex) {
        pendingWrittenSteps.remove(stepIndex);
    }

    int pendingWriteCount() {
        return pendingWrittenSteps.size();
    }

    void rememberPendingWrites(final MelodicPattern pattern) {
        pendingWrittenSteps.clear();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.active() && !step.tieFromPrevious() && step.pitch() != null) {
                pendingWrittenSteps.put(i, step);
            }
        }
    }
}
