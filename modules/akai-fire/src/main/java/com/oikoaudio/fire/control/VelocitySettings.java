package com.oikoaudio.fire.control;

public final class VelocitySettings {
    private final int initialCenterVelocity;
    private final int initialSensitivity;
    private final int minCenterVelocity;
    private final int maxCenterVelocity;

    private int centerVelocity;
    private int sensitivity;

    public VelocitySettings(final int centerVelocity, final int minCenterVelocity, final int maxCenterVelocity,
                            final int sensitivity) {
        if (minCenterVelocity > maxCenterVelocity) {
            throw new IllegalArgumentException("Minimum center velocity must not exceed maximum center velocity");
        }
        this.minCenterVelocity = minCenterVelocity;
        this.maxCenterVelocity = maxCenterVelocity;
        this.initialCenterVelocity = clampCenterVelocity(centerVelocity);
        this.initialSensitivity = LiveVelocityLogic.clampSensitivity(sensitivity);
        reset();
    }

    public int resolveVelocity(final int rawVelocity) {
        return LiveVelocityLogic.resolveVelocity(centerVelocity, sensitivity, rawVelocity);
    }

    public boolean adjustCenterVelocity(final int amount) {
        final int nextVelocity = clampCenterVelocity(centerVelocity + amount);
        if (nextVelocity == centerVelocity) {
            return false;
        }
        centerVelocity = nextVelocity;
        return true;
    }

    public boolean adjustSensitivity(final int amount) {
        final int nextSensitivity = LiveVelocityLogic.clampSensitivity(sensitivity + amount);
        if (nextSensitivity == sensitivity) {
            return false;
        }
        sensitivity = nextSensitivity;
        return true;
    }

    public void reset() {
        centerVelocity = initialCenterVelocity;
        sensitivity = initialSensitivity;
    }

    public int centerVelocity() {
        return centerVelocity;
    }

    public int sensitivity() {
        return sensitivity;
    }

    public int minCenterVelocity() {
        return minCenterVelocity;
    }

    public int maxCenterVelocity() {
        return maxCenterVelocity;
    }

    public String summary() {
        return "Sens %d%% / Ctr %d".formatted(sensitivity, centerVelocity);
    }

    private int clampCenterVelocity(final int velocity) {
        return Math.max(minCenterVelocity, Math.min(maxCenterVelocity, velocity));
    }
}
