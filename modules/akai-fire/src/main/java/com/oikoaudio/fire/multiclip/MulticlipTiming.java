package com.oikoaudio.fire.multiclip;

/** Step-based timing conversions used by Multiclip Seq. */
public final class MulticlipTiming {
    public static final int MIN_LOOP_STEPS = 1;
    public static final int MAX_LOOP_STEPS = 256;
    public static final double STEP_BEATS = 0.25;
    public static final double FINE_STEP_BEATS = 1.0 / 64.0;

    private MulticlipTiming() {}

    public static double beatsForSteps(final int steps) {
        return steps * STEP_BEATS;
    }

    public static int stepsForBeats(final double beats) {
        return Math.max(MIN_LOOP_STEPS, (int) Math.round(beats / STEP_BEATS));
    }

    public static int adjustLoopSteps(final int current, final int delta) {
        return Math.max(MIN_LOOP_STEPS, Math.min(MAX_LOOP_STEPS, current + delta));
    }

    public static int visibleLoopStepCount(
            final double loopBeats, final int firstVisibleStep, final int visibleStepCount) {
        final int remaining = stepsForBeats(loopBeats) - Math.max(0, firstVisibleStep);
        return Math.max(0, Math.min(Math.max(0, visibleStepCount), remaining));
    }
}
