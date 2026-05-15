package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.lights.RgbLigthState;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DrumStepPadSurface {
    private final DrumStepPadState state = new DrumStepPadState();
    private final RecurrencePadInteraction recurrencePads = new RecurrencePadInteraction(true);

    enum StepReleaseAction {
        NONE,
        CLEAR_STEP
    }

    enum StepPressAction {
        NONE,
        FIXED_LENGTH,
        COPY,
        ACCENT,
        ADD_STEP
    }

    boolean isAnyStepHeld() {
        return state.isAnyStepHeld();
    }

    void pressStep(final int stepIndex) {
        beginRecurrenceHoldIfNeeded();
        state.addHeldStep(stepIndex);
    }

    void releaseStep(final int stepIndex) {
        state.removeHeldStep(stepIndex);
        if (!isAnyStepHeld()) {
            clearRecurrenceHold();
        }
    }

    void cancelPressedStep(final int stepIndex) {
        releaseStep(stepIndex);
    }

    StepReleaseAction handleStepRelease(final int stepIndex,
                                        final NoteStep note,
                                        final boolean copyHeld,
                                        final boolean fixedLengthHeld,
                                        final boolean accentGesture) {
        releaseStep(stepIndex);
        if (copyHeld || fixedLengthHeld) {
            state.removeAdded(stepIndex);
            return StepReleaseAction.NONE;
        }
        if (accentGesture) {
            state.clearGestureConsumed(stepIndex);
            state.removeAdded(stepIndex);
            return StepReleaseAction.NONE;
        }
        if (state.consumeGesture(stepIndex)) {
            state.removeAdded(stepIndex);
            return StepReleaseAction.NONE;
        }
        if (note != null && note.state() == NoteStep.State.NoteOn && !state.isAdded(stepIndex)) {
            if (!state.isModified(stepIndex)) {
                state.removeAdded(stepIndex);
                return StepReleaseAction.CLEAR_STEP;
            }
            state.removeModified(stepIndex);
        }
        state.removeAdded(stepIndex);
        return StepReleaseAction.NONE;
    }

    StepPressAction handleStepPress(final int stepIndex,
                                    final NoteStep note,
                                    final boolean fixedLengthHeld,
                                    final boolean copyHeld,
                                    final boolean accentGesture) {
        pressStep(stepIndex);
        if (fixedLengthHeld) {
            return StepPressAction.FIXED_LENGTH;
        }
        if (copyHeld) {
            return StepPressAction.COPY;
        }
        if (accentGesture) {
            return StepPressAction.ACCENT;
        }
        if (note == null || note.state() == NoteStep.State.Empty || note.state() == NoteStep.State.NoteSustain) {
            return StepPressAction.ADD_STEP;
        }
        return StepPressAction.NONE;
    }

    void beginRecurrenceHoldIfNeeded() {
        recurrencePads.beginHoldIfNeeded(isAnyStepHeld());
    }

    void clearRecurrenceHold() {
        recurrencePads.clearHold();
    }

    boolean shouldShowRecurrenceRow() {
        return recurrencePads.shouldShowRow(isAnyStepHeld());
    }

    boolean handleRecurrencePadPress(final int padIndex,
                                     final boolean pressed,
                                     final List<NoteStep> targets,
                                     final Runnable markConsumed,
                                     final java.util.function.IntConsumer togglePad,
                                     final java.util.function.IntConsumer applySpan) {
        if (targets.isEmpty()) {
            return true;
        }
        final RecurrencePattern recurrence = RecurrencePattern.of(
                targets.get(0).recurrenceLength(), targets.get(0).recurrenceMask());
        return recurrencePads.handlePadPress(padIndex, pressed, true, recurrence,
                markConsumed, togglePad, applySpan);
    }

    RgbLigthState recurrencePadLight(final int padIndex,
                                     final List<NoteStep> targets,
                                     final RgbLigthState color,
                                     final RgbLigthState fallback) {
        if (targets.isEmpty()) {
            return fallback;
        }
        final NoteStep note = targets.get(0);
        return recurrencePads.padLight(padIndex,
                RecurrencePattern.of(note.recurrenceLength(), note.recurrenceMask()),
                color);
    }

    RgbLigthState stepPadLight(final int stepIndex,
                               final int availableSteps,
                               final NoteStep noteStep,
                               final int playingStep,
                               final boolean lengthDisplay,
                               final NoteStep copyNote,
                               final int blinkState,
                               final RgbLigthState currentPadColor,
                               final int accentedVelocity) {
        if (stepIndex >= availableSteps) {
            return RgbLigthState.OFF;
        }
        final NoteStep.State state = noteStep == null ? NoteStep.State.Empty : noteStep.state();

        if (state == NoteStep.State.Empty) {
            return emptyStepLight(stepIndex, playingStep);
        }
        if (state == NoteStep.State.NoteSustain) {
            if (lengthDisplay) {
                if (stepIndex == playingStep) {
                    return currentPadColor.getBrightend();
                }
                return currentPadColor.getVeryDimmed();
            }
            return emptyStepLight(stepIndex, playingStep);
        }
        if (copyNote != null && copyNote.x() == stepIndex) {
            if (blinkState % 4 < 2) {
                return RgbLigthState.GRAY_1;
            }
            return currentPadColor;
        }
        return occupiedStepLight(noteStep, stepIndex == playingStep, currentPadColor, accentedVelocity);
    }

    private static RgbLigthState occupiedStepLight(final NoteStep noteStep,
                                                  final boolean playing,
                                                  final RgbLigthState currentPadColor,
                                                  final int accentedVelocity) {
        if (noteStep == null) {
            return StepPadLightHelper.renderOccupiedStep(currentPadColor, false, playing);
        }

        final int velocity = (int) Math.round(noteStep.velocity() * 127);
        return StepPadLightHelper.renderOccupiedStep(currentPadColor, velocity >= accentedVelocity, playing);
    }

    private static RgbLigthState emptyStepLight(final int stepIndex, final int playingStep) {
        return StepPadLightHelper.renderEmptyStep(stepIndex, playingStep);
    }

    IntSetValue heldStepsValue() {
        return state.heldStepsValue();
    }

    Stream<Integer> heldStepStream() {
        return state.heldStepStream();
    }

    List<NoteStep> heldNotes(final NoteStep[] assignments) {
        return state.heldStepStream()
                .filter(idx -> idx >= 0 && idx < assignments.length)
                .map(idx -> assignments[idx])
                .filter(ns -> ns != null && ns.state() == NoteStep.State.NoteOn)
                .collect(Collectors.toList());
    }

    boolean shouldApplyDefaultsToObservedStep(final NoteStep noteStep) {
        return state.isAdded(noteStep.x()) && noteStep.state() == NoteStep.State.NoteOn;
    }

    void markAdded(final int stepIndex) {
        state.markAdded(stepIndex);
    }

    void markModified(final int stepIndex) {
        state.markModified(stepIndex);
    }

    void markModified(final NoteStep noteStep) {
        state.markModified(noteStep);
    }

    void markGestureConsumed(final int stepIndex) {
        state.markGestureConsumed(stepIndex);
    }
}
