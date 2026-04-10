package com.oikoaudio.fire.note;

final class LiveVelocityLogic {
    private LiveVelocityLogic() {
    }

    static int resolveVelocity(final int defaultVelocity, final int sensitivityPercent, final int rawVelocity) {
        final int clampedDefault = clampVelocity(defaultVelocity);
        final int clampedSensitivity = Math.max(0, Math.min(100, sensitivityPercent));
        final int clampedRaw = clampVelocity(rawVelocity);
        final double blend = clampedSensitivity / 100.0;
        return clampVelocity((int) Math.round(clampedDefault + (clampedRaw - clampedDefault) * blend));
    }

    static int clampVelocity(final int velocity) {
        return Math.max(1, Math.min(127, velocity));
    }

    static int clampSensitivity(final int sensitivityPercent) {
        return Math.max(0, Math.min(100, sensitivityPercent));
    }
}
