package com.oikoaudio.fire.control;

public class TouchResetGesture {
    private final long holdMs;
    private final long recentAdjustmentSuppressMs;
    private final int toleratedAdjustmentUnits;
    private final long[] touchStartedAt;
    private final long[] adjustmentUnitsAtTouchStart;
    private final long[] adjustmentUnits;
    private final long[] lastAdjustedAt;

    public TouchResetGesture(final int encoderCount, final long holdMs, final long recentAdjustmentSuppressMs,
                             final int toleratedAdjustmentUnits) {
        this.holdMs = holdMs;
        this.recentAdjustmentSuppressMs = recentAdjustmentSuppressMs;
        this.toleratedAdjustmentUnits = toleratedAdjustmentUnits;
        this.touchStartedAt = new long[encoderCount];
        this.adjustmentUnitsAtTouchStart = new long[encoderCount];
        this.adjustmentUnits = new long[encoderCount];
        this.lastAdjustedAt = new long[encoderCount];
    }

    public void onTouchStart(final int encoderIndex) {
        touchStartedAt[encoderIndex] = System.currentTimeMillis();
        adjustmentUnitsAtTouchStart[encoderIndex] = adjustmentUnits[encoderIndex];
    }

    public void onAdjusted(final int encoderIndex) {
        onAdjusted(encoderIndex, 1);
    }

    public void onAdjusted(final int encoderIndex, final int units) {
        adjustmentUnits[encoderIndex] += Math.max(1, units);
        lastAdjustedAt[encoderIndex] = System.currentTimeMillis();
    }

    public boolean shouldResetOnTouchRelease(final int encoderIndex) {
        final long now = System.currentTimeMillis();
        final long touchStart = touchStartedAt[encoderIndex];
        touchStartedAt[encoderIndex] = 0L;
        if (touchStart <= 0L) {
            return false;
        }

        final long touchDuration = now - touchStart;
        if (touchDuration < holdMs) {
            return false;
        }

        if (adjustmentUnits[encoderIndex] - adjustmentUnitsAtTouchStart[encoderIndex] > toleratedAdjustmentUnits) {
            return false;
        }

        return lastAdjustedAt[encoderIndex] + recentAdjustmentSuppressMs < touchStart;
    }
}
