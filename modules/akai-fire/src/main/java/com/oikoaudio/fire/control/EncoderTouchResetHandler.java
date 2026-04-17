package com.oikoaudio.fire.control;

import java.util.function.BooleanSupplier;

/**
 * Shared touch-hold reset orchestration for encoder interactions.
 */
public final class EncoderTouchResetHandler {
    private final TouchResetGesture gesture;
    private final BooleanSupplier resetEnabled;
    private final Scheduler scheduler;
    private final long holdMs;
    private final Runnable clearDisplay;

    public EncoderTouchResetHandler(final TouchResetGesture gesture,
                                    final BooleanSupplier resetEnabled,
                                    final Scheduler scheduler,
                                    final long holdMs,
                                    final Runnable clearDisplay) {
        this.gesture = gesture;
        this.resetEnabled = resetEnabled;
        this.scheduler = scheduler;
        this.holdMs = holdMs;
        this.clearDisplay = clearDisplay;
    }

    public void handleResettableTouch(final int encoderIndex, final boolean touched,
                                      final Runnable showInfo, final Runnable resetAction) {
        if (touched) {
            beginTouchReset(encoderIndex, () -> {
                resetAction.run();
                showInfo.run();
            });
            showInfo.run();
            return;
        }
        endTouchReset(encoderIndex);
        clearDisplay.run();
    }

    public void beginTouchReset(final int encoderIndex, final Runnable resetAction) {
        if (!resetEnabled.getAsBoolean()) {
            return;
        }
        gesture.onTouchStart(encoderIndex);
        scheduler.schedule(() -> {
            if (resetEnabled.getAsBoolean() && gesture.shouldResetWhileTouched(encoderIndex)) {
                resetAction.run();
            }
        }, holdMs);
    }

    public void markAdjusted(final int encoderIndex) {
        markAdjusted(encoderIndex, 1);
    }

    public void markAdjusted(final int encoderIndex, final int units) {
        if (resetEnabled.getAsBoolean()) {
            gesture.onAdjusted(encoderIndex, units);
        }
    }

    public void endTouchReset(final int encoderIndex) {
        if (resetEnabled.getAsBoolean()) {
            gesture.onTouchEnd(encoderIndex);
        }
    }

    @FunctionalInterface
    public interface Scheduler {
        void schedule(Runnable task, long delayMs);
    }
}
