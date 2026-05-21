package com.oikoaudio.fire.display;

public final class VuMeterPeakHold {
    private static final int HOLD_TICKS = 5;
    private static final int DECAY_UNITS = 2;

    private final int[] values;
    private final int[] holdTicks;

    public VuMeterPeakHold(final int size) {
        values = new int[size];
        holdTicks = new int[size];
    }

    public void update(final int index, final int value) {
        if (index < 0 || index >= values.length) {
            return;
        }
        final int clamped = VuMeterFormatter.clamp(value, 0, VuMeterFormatter.RANGE - 1);
        if (clamped >= values[index]) {
            values[index] = clamped;
            holdTicks[index] = HOLD_TICKS;
        }
    }

    public void decay() {
        for (int i = 0; i < values.length; i++) {
            if (holdTicks[i] > 0) {
                holdTicks[i]--;
                continue;
            }
            values[i] = Math.max(0, values[i] - DECAY_UNITS);
        }
    }

    public int valueAt(final int index) {
        return index >= 0 && index < values.length ? values[index] : 0;
    }

    public void reset(final int index) {
        if (index < 0 || index >= values.length) {
            return;
        }
        values[index] = 0;
        holdTicks[index] = 0;
    }
}
