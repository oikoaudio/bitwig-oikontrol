package com.oikoaudio.fire.values;

/** Coordinate conversions between Bitwig's absolute clip timeline and a clip's loop window. */
public final class ClipLoopWindow {
    private static final double EPSILON = 0.0001;

    private ClipLoopWindow() {}

    public static int startStep(final double loopStart, final double stepLength) {
        if (stepLength <= 0.0) {
            return 0;
        }
        return Math.max(0, (int) Math.round(loopStart / stepLength));
    }

    public static int relativeStep(
            final int absoluteStep, final double loopStart, final double stepLength) {
        return absoluteStep < 0 ? -1 : absoluteStep - startStep(loopStart, stepLength);
    }

    public static double relativeBeat(
            final double absoluteBeat, final double loopStart, final double loopLength) {
        final double length = Math.max(EPSILON, loopLength);
        final double relative = (absoluteBeat - loopStart) % length;
        return relative < 0.0 ? relative + length : relative;
    }

    public static double absoluteBeat(
            final double relativeBeat, final double loopStart, final double loopLength) {
        return Math.max(0.0, loopStart) + relativeBeat(relativeBeat, 0.0, loopLength);
    }

    public static double movePlayStart(
            final double playStart,
            final double loopStart,
            final double loopLength,
            final double deltaBeats) {
        return absoluteBeat(
                relativeBeat(playStart, loopStart, loopLength) + deltaBeats, loopStart, loopLength);
    }
}
