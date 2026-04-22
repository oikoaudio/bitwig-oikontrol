package com.oikoaudio.fire.fugue;

public final class FuguePitchIntervals {
    private static final int MIN_DEGREES = -24;
    private static final int MAX_DEGREES = 24;
    private static final int OCTAVE_DEGREES = 7;

    private FuguePitchIntervals() {
    }

    public static int nextDegreeInterval(final int current, final int amount) {
        return clamp(current + amount);
    }

    public static int octaveJump(final int current, final int amount) {
        return clamp(current + amount * OCTAVE_DEGREES);
    }

    public static String label(final int degreeOffset) {
        return degreeOffset == 0 ? "0d" : "%+dd".formatted(degreeOffset);
    }

    private static int clamp(final int value) {
        return Math.max(MIN_DEGREES, Math.min(MAX_DEGREES, value));
    }
}
