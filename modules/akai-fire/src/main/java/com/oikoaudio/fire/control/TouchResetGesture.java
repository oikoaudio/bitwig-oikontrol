package com.oikoaudio.fire.control;

public class TouchResetGesture {
    private final long holdMs;
    private final long recentAdjustmentSuppressMs;
    private final int toleratedAdjustmentUnits;
    private final long[] touchStartedAt;
    private final long[] adjustmentUnitsAtTouchStart;
    private final long[] adjustmentUnits;
    private final long[] lastAdjustedAt;
    private final boolean[] touchActive;
    private final boolean[] resetTriggered;

    public TouchResetGesture(final int encoderCount, final long holdMs, final long recentAdjustmentSuppressMs,
                             final int toleratedAdjustmentUnits) {
        this.holdMs = holdMs;
        this.recentAdjustmentSuppressMs = recentAdjustmentSuppressMs;
        this.toleratedAdjustmentUnits = toleratedAdjustmentUnits;
        this.touchStartedAt = new long[encoderCount];
        this.adjustmentUnitsAtTouchStart = new long[encoderCount];
        this.adjustmentUnits = new long[encoderCount];
        this.lastAdjustedAt = new long[encoderCount];
        this.touchActive = new boolean[encoderCount];
        this.resetTriggered = new boolean[encoderCount];
    }

    public void onTouchStart(final int encoderIndex) {
        touchStartedAt[encoderIndex] = System.currentTimeMillis();
        adjustmentUnitsAtTouchStart[encoderIndex] = adjustmentUnits[encoderIndex];
        touchActive[encoderIndex] = true;
        resetTriggered[encoderIndex] = false;
    }

    public void onAdjusted(final int encoderIndex) {
        onAdjusted(encoderIndex, 1);
    }

    public void onAdjusted(final int encoderIndex, final int units) {
        adjustmentUnits[encoderIndex] += Math.max(1, units);
        lastAdjustedAt[encoderIndex] = System.currentTimeMillis();
    }

    public boolean shouldResetWhileTouched(final int encoderIndex) {
        final long now = System.currentTimeMillis();
        final long touchStart = touchStartedAt[encoderIndex];
        if (!touchActive[encoderIndex] || resetTriggered[encoderIndex] || touchStart <= 0L) {
            return false;
        }

        final long touchDuration = now - touchStart;
        if (touchDuration < holdMs) {
            return false;
        }

        if (adjustmentUnits[encoderIndex] - adjustmentUnitsAtTouchStart[encoderIndex] > toleratedAdjustmentUnits) {
            return false;
        }

        final boolean shouldReset = lastAdjustedAt[encoderIndex] + recentAdjustmentSuppressMs < touchStart;
        if (shouldReset) {
            resetTriggered[encoderIndex] = true;
        }
        return shouldReset;
    }

    public void onTouchEnd(final int encoderIndex) {
        touchStartedAt[encoderIndex] = 0L;
        touchActive[encoderIndex] = false;
        resetTriggered[encoderIndex] = false;
    }
}
