package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.lights.RgbLigthState;

import java.util.List;
import java.util.function.IntConsumer;

public final class HeldStepRecurrenceRow {
    private final RecurrencePadInteraction recurrencePads = new RecurrencePadInteraction(true);

    public void beginHoldIfNeeded(final boolean hasHeldSteps) {
        recurrencePads.beginHoldIfNeeded(hasHeldSteps);
    }

    public void clearHold() {
        recurrencePads.clearHold();
    }

    public boolean shouldShow(final boolean hasHeldSteps) {
        return recurrencePads.shouldShowRow(hasHeldSteps);
    }

    public boolean handlePadPress(final int padIndex,
                                  final boolean pressed,
                                  final List<NoteStep> targets,
                                  final Runnable markConsumed,
                                  final IntConsumer togglePad,
                                  final IntConsumer applySpan) {
        if (targets.isEmpty()) {
            return true;
        }
        return handlePadPress(padIndex, pressed, true, recurrenceOf(targets.get(0)),
                markConsumed, togglePad, applySpan);
    }

    public boolean handlePadPress(final int padIndex,
                                  final boolean pressed,
                                  final boolean hasTarget,
                                  final RecurrencePattern recurrence,
                                  final Runnable markConsumed,
                                  final IntConsumer togglePad,
                                  final IntConsumer applySpan) {
        return recurrencePads.handlePadPress(padIndex, pressed, hasTarget, recurrence,
                markConsumed, togglePad, applySpan);
    }

    public RgbLigthState padLight(final int padIndex,
                                  final List<NoteStep> targets,
                                  final RgbLigthState color,
                                  final RgbLigthState fallback) {
        if (targets.isEmpty()) {
            return fallback;
        }
        return recurrencePads.padLight(padIndex, recurrenceOf(targets.get(0)), color);
    }

    public RgbLigthState padLight(final int padIndex,
                                  final RecurrencePattern recurrence,
                                  final RgbLigthState color) {
        return recurrencePads.padLight(padIndex, recurrence, color);
    }

    private static RecurrencePattern recurrenceOf(final NoteStep note) {
        return RecurrencePattern.of(note.recurrenceLength(), note.recurrenceMask());
    }
}
