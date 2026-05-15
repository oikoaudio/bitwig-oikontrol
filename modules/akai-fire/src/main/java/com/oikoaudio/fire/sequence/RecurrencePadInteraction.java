package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.RgbLigthState;

import java.util.function.IntConsumer;

public final class RecurrencePadInteraction {
    public static final long DEFAULT_LIGHT_HOLD_MS = 180L;

    private final boolean spanAnchorEnabled;
    private final long lightHoldMs;
    private boolean spanAnchorHeld = false;
    private boolean spanGestureUsed = false;
    private long holdStartedAtMs = 0L;

    public RecurrencePadInteraction(final boolean spanAnchorEnabled) {
        this(spanAnchorEnabled, DEFAULT_LIGHT_HOLD_MS);
    }

    public RecurrencePadInteraction(final boolean spanAnchorEnabled, final long lightHoldMs) {
        this.spanAnchorEnabled = spanAnchorEnabled;
        this.lightHoldMs = Math.max(0L, lightHoldMs);
    }

    public void beginHoldIfNeeded(final boolean alreadyHeld) {
        if (!alreadyHeld) {
            holdStartedAtMs = System.currentTimeMillis();
        }
    }

    public void clearHold() {
        spanAnchorHeld = false;
        spanGestureUsed = false;
        holdStartedAtMs = 0L;
    }

    public boolean shouldShowRow(final boolean hasHeldTarget) {
        return hasHeldTarget
                && holdStartedAtMs > 0L
                && System.currentTimeMillis() - holdStartedAtMs >= lightHoldMs;
    }

    public boolean handlePadPress(final int padIndex,
                                  final boolean pressed,
                                  final boolean hasTarget,
                                  final RecurrencePattern recurrence,
                                  final Runnable markConsumed,
                                  final IntConsumer togglePad,
                                  final IntConsumer applySpan) {
        if (!hasTarget || padIndex >= RecurrencePattern.EDITOR_DEFAULT_SPAN) {
            return false;
        }
        if (spanAnchorEnabled && padIndex == 0 && !pressed && spanAnchorHeld) {
            spanAnchorHeld = false;
            final boolean usedSpanGesture = spanGestureUsed;
            spanGestureUsed = false;
            if (!usedSpanGesture) {
                markConsumed.run();
                togglePad.accept(0);
            }
            return true;
        }
        if (!pressed) {
            return true;
        }
        markConsumed.run();
        if (spanAnchorEnabled && padIndex == 0) {
            spanAnchorHeld = true;
            spanGestureUsed = false;
            return true;
        }
        if (spanAnchorEnabled && spanAnchorHeld) {
            spanGestureUsed = true;
            applySpan.accept(padIndex + 1);
            return true;
        }
        if (padIndex >= recurrence.effectiveSpan()) {
            return true;
        }
        togglePad.accept(padIndex);
        return true;
    }

    public RgbLigthState padLight(final int padIndex,
                                  final RecurrencePattern recurrence,
                                  final RgbLigthState baseColor) {
        if (padIndex >= RecurrencePattern.EDITOR_DEFAULT_SPAN) {
            return RgbLigthState.OFF;
        }
        final int span = recurrence.effectiveSpan();
        if (padIndex >= span) {
            return RgbLigthState.OFF;
        }
        final int mask = recurrence.effectiveMask(span);
        return ((mask >> padIndex) & 0x1) == 1 ? baseColor.getBrightend() : baseColor.getDimmed();
    }
}
