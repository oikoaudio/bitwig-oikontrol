package com.oikoaudio.fire.note;

import com.oikoaudio.fire.control.TouchResetGesture;

import java.util.function.BooleanSupplier;

/**
 * Owns touch-hold reset and recent-adjustment suppression for encoder interactions.
 */
class NoteEncoderTouchResetHandler {
    private final TouchResetGesture gesture;
    private final BooleanSupplier resetEnabled;
    private final Scheduler scheduler;
    private final long holdMs;
    private final Runnable clearDisplay;

    NoteEncoderTouchResetHandler(final TouchResetGesture gesture,
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

    void handleResettableTouch(final int encoderIndex, final boolean touched,
                               final Runnable showInfo, final Runnable resetAction) {
        if (touched) {
            if (resetEnabled.getAsBoolean()) {
                gesture.onTouchStart(encoderIndex);
                scheduler.schedule(() -> {
                    if (resetEnabled.getAsBoolean() && gesture.shouldResetWhileTouched(encoderIndex)) {
                        resetAction.run();
                        showInfo.run();
                    }
                }, holdMs);
            }
            showInfo.run();
            return;
        }
        if (resetEnabled.getAsBoolean()) {
            gesture.onTouchEnd(encoderIndex);
        }
        clearDisplay.run();
    }

    void markAdjusted(final int encoderIndex) {
        if (resetEnabled.getAsBoolean()) {
            gesture.onAdjusted(encoderIndex, 1);
        }
    }

    @FunctionalInterface
    interface Scheduler {
        void schedule(Runnable task, long delayMs);
    }
}
