package com.oikoaudio.fire.multiclip;

record MulticlipEuclidState(int length, int pulses, int rotation, int accentPulses) {
    static MulticlipEuclidState defaults() {
        return new MulticlipEuclidState(16, 0, 0, 0);
    }

    MulticlipEuclidState adjusted(final int parameterIndex, final int delta) {
        int nextLength = length;
        int nextPulses = pulses;
        int nextRotation = rotation;
        int nextAccentPulses = accentPulses;
        switch (parameterIndex) {
            case 0 -> nextLength = clamp(length + delta, 1, 16);
            case 1 -> nextPulses = clamp(pulses + delta, 0, length);
            case 2 -> nextRotation = Math.floorMod(rotation + delta, length);
            case 3 -> nextAccentPulses = clamp(accentPulses + delta, 0, pulses);
            default -> {
                return this;
            }
        }
        nextPulses = Math.min(nextPulses, nextLength);
        nextRotation = Math.floorMod(nextRotation, nextLength);
        nextAccentPulses = Math.min(nextAccentPulses, nextPulses);
        return new MulticlipEuclidState(nextLength, nextPulses, nextRotation, nextAccentPulses);
    }

    boolean[] pattern() {
        final boolean[] unrotated = new boolean[length];
        for (int step = 0; step < length; step++) {
            unrotated[step] = pulses > 0 && (step * pulses) % length < pulses;
        }
        final boolean[] rotated = new boolean[length];
        for (int step = 0; step < length; step++) {
            rotated[step] = unrotated[Math.floorMod(step - rotation, length)];
        }
        return rotated;
    }

    private static int clamp(final int value, final int minimum, final int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
