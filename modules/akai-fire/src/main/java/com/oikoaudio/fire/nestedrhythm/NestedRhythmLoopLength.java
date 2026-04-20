package com.oikoaudio.fire.nestedrhythm;

final class NestedRhythmLoopLength {
    static final int STEP_COUNT = 32;

    private NestedRhythmLoopLength() {
    }

    static int normalizeLastStepIndex(final int stepIndex) {
        return Math.max(0, Math.min(STEP_COUNT - 1, stepIndex));
    }

    static int loopFineSteps(final int totalFineSteps, final int lastStepIndex) {
        final int clampedTotalFineSteps = Math.max(1, totalFineSteps);
        final int normalizedLastStepIndex = normalizeLastStepIndex(lastStepIndex);
        return Math.max(1, (int) Math.round(
                clampedTotalFineSteps * ((normalizedLastStepIndex + 1) / (double) STEP_COUNT)));
    }

    static double loopLengthBeats(final double totalBeats, final int lastStepIndex) {
        final double clampedTotalBeats = Math.max(0.25, totalBeats);
        return clampedTotalBeats * (normalizeLastStepIndex(lastStepIndex) + 1) / STEP_COUNT;
    }
}
