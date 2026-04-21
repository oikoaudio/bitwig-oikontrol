package com.oikoaudio.fire.fugue;

public final class FuguePitchIntervals {
    private static final int MIN_DEGREES = -24;
    private static final int MAX_DEGREES = 24;
    private static final int OCTAVE_DEGREES = 7;
    private static final int[] NOMINAL_MAJOR_SEMITONES = {0, 2, 4, 5, 7, 9, 11};

    private FuguePitchIntervals() {
    }

    public static int nextDegreeInterval(final int current, final int amount) {
        return clamp(current + amount);
    }

    public static int octaveJump(final int current, final int amount) {
        return clamp(current + amount * OCTAVE_DEGREES);
    }

    public static String label(final int degreeOffset) {
        final int semitones = nominalSemitoneOffset(degreeOffset);
        return semitones == 0 ? "0" : "%+d".formatted(semitones);
    }

    private static int nominalSemitoneOffset(final int degreeOffset) {
        final int sign = degreeOffset < 0 ? -1 : 1;
        final int absolute = Math.abs(degreeOffset);
        final int octaves = absolute / OCTAVE_DEGREES;
        final int degree = absolute % OCTAVE_DEGREES;
        return sign * (octaves * 12 + NOMINAL_MAJOR_SEMITONES[degree]);
    }

    private static int clamp(final int value) {
        return Math.max(MIN_DEGREES, Math.min(MAX_DEGREES, value));
    }
}
