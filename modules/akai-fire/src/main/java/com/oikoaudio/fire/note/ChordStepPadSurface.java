package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.RecurrencePadInteraction;
import com.oikoaudio.fire.sequence.RecurrencePattern;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

final class ChordStepPadSurface {
    enum ModifierPressAction {
        NONE,
        SELECT,
        FIXED_LENGTH,
        COPY,
        DELETE
    }

    enum RangePressAction {
        NONE,
        EXTEND,
        BLOCK
    }

    enum StepPressAction {
        HOLD_EXISTING,
        ADD_STEP,
        LOAD_BUILDER
    }

    enum StepReleaseAction {
        NONE,
        CLEAR_STEP
    }

    private final RecurrencePadInteraction recurrencePads = new RecurrencePadInteraction(true);
    private final Set<Integer> heldStepPads = new HashSet<>();
    private final Set<Integer> addedStepPads = new HashSet<>();
    private final Set<Integer> modifiedStepPads = new HashSet<>();
    private final Set<Integer> modifierHandledStepPads = new HashSet<>();
    private Integer heldStepAnchor = null;

    void beginRecurrenceHoldIfNeeded() {
        recurrencePads.beginHoldIfNeeded(hasHeldSteps());
    }

    void clearRecurrenceHold() {
        recurrencePads.clearHold();
    }

    boolean hasHeldSteps() {
        return !heldStepPads.isEmpty();
    }

    boolean hasHeldStep(final int stepIndex) {
        return heldStepPads.contains(stepIndex);
    }

    Set<Integer> heldStepSnapshot() {
        return Set.copyOf(heldStepPads);
    }

    void addHeldStep(final int stepIndex) {
        heldStepPads.add(stepIndex);
    }

    void removeHeldStep(final int stepIndex) {
        heldStepPads.remove(stepIndex);
        if (heldStepPads.isEmpty()) {
            clearRecurrenceHold();
        }
    }

    void markAddedStep(final int stepIndex) {
        addedStepPads.add(stepIndex);
    }

    boolean consumeAddedStep(final int stepIndex) {
        return addedStepPads.remove(stepIndex);
    }

    void markModifiedStep(final int stepIndex) {
        modifiedStepPads.add(stepIndex);
    }

    void markModifiedSteps(final Collection<Integer> stepIndices) {
        modifiedStepPads.addAll(stepIndices);
    }

    void markModifiedNotes(final List<NoteStep> notes) {
        notes.forEach(note -> markModifiedStep(note.x()));
    }

    boolean consumeModifiedStep(final int stepIndex) {
        return modifiedStepPads.remove(stepIndex);
    }

    void markModifierHandledStep(final int stepIndex) {
        modifierHandledStepPads.add(stepIndex);
    }

    boolean consumeModifierHandledStep(final int stepIndex) {
        return modifierHandledStepPads.remove(stepIndex);
    }

    boolean hasAddedStep(final int stepIndex) {
        return addedStepPads.contains(stepIndex);
    }

    RangePressAction rangePressAction(final int stepIndex, final boolean canExtendFromAnchor) {
        if (heldStepAnchor == null || heldStepAnchor == stepIndex || !heldStepPads.contains(heldStepAnchor)) {
            return RangePressAction.NONE;
        }
        return canExtendFromAnchor ? RangePressAction.EXTEND : RangePressAction.BLOCK;
    }

    void markRangeExtended(final int stepIndex) {
        if (heldStepAnchor != null) {
            heldStepPads.add(stepIndex);
            modifiedStepPads.add(heldStepAnchor);
            modifiedStepPads.add(stepIndex);
        }
    }

    StepPressAction stepPressAction(final int stepIndex,
                                    final boolean hasStepStartNote,
                                    final boolean builderFamily) {
        beginRecurrenceHoldIfNeeded();
        addHeldStep(stepIndex);
        if (heldStepAnchor == null) {
            heldStepAnchor = stepIndex;
        }
        if (!hasStepStartNote) {
            return StepPressAction.ADD_STEP;
        }
        if (builderFamily) {
            return StepPressAction.LOAD_BUILDER;
        }
        return StepPressAction.HOLD_EXISTING;
    }

    void cancelStepPress(final int stepIndex) {
        removeHeldStep(stepIndex);
        refreshHeldStepAnchor(stepIndex);
    }

    StepReleaseAction stepReleaseAction(final int stepIndex, final boolean hasStepStartNote) {
        removeHeldStep(stepIndex);
        refreshHeldStepAnchor(stepIndex);
        if (consumeModifiedStep(stepIndex)) {
            return StepReleaseAction.NONE;
        }
        if (consumeAddedStep(stepIndex)) {
            return StepReleaseAction.NONE;
        }
        return hasStepStartNote ? StepReleaseAction.CLEAR_STEP : StepReleaseAction.NONE;
    }

    ModifierPressAction modifierPressAction(final boolean selectHeld,
                                            final boolean fixedLengthHeld,
                                            final boolean copyHeld,
                                            final boolean deleteHeld) {
        if (selectHeld) {
            return ModifierPressAction.SELECT;
        }
        if (fixedLengthHeld) {
            return ModifierPressAction.FIXED_LENGTH;
        }
        if (copyHeld) {
            return ModifierPressAction.COPY;
        }
        if (deleteHeld) {
            return ModifierPressAction.DELETE;
        }
        return ModifierPressAction.NONE;
    }

    Integer heldStepAnchor() {
        return heldStepAnchor;
    }

    void setHeldStepAnchor(final Integer heldStepAnchor) {
        this.heldStepAnchor = heldStepAnchor;
    }

    void refreshHeldStepAnchor(final int releasedStepIndex) {
        if (heldStepAnchor == null || heldStepAnchor != releasedStepIndex) {
            return;
        }
        heldStepAnchor = heldStepPads.stream().findFirst().orElse(null);
    }

    void clearStepTracking() {
        heldStepPads.clear();
        addedStepPads.clear();
        modifiedStepPads.clear();
        modifierHandledStepPads.clear();
        heldStepAnchor = null;
        clearRecurrenceHold();
    }

    boolean shouldShowRecurrenceRow() {
        return recurrencePads.shouldShowRow(hasHeldSteps());
    }

    boolean handleRecurrencePadPress(final int padIndex,
                                     final boolean pressed,
                                     final List<NoteStep> targets,
                                     final Runnable markConsumed,
                                     final IntConsumer togglePad,
                                     final IntConsumer applySpan) {
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
                               final boolean occupied,
                               final boolean accented,
                               final boolean sustained,
                               final int playingStep,
                               final RgbLigthState occupiedStepColor,
                               final RgbLigthState sustainedStepColor,
                               final RgbLigthState heldStepColor) {
        if (!StepPadLightHelper.isStepWithinVisibleLoop(stepIndex, availableSteps)) {
            return RgbLigthState.OFF;
        }
        if (hasHeldStep(stepIndex)) {
            return heldStepColor.getBrightest();
        }
        if (occupied) {
            if (stepIndex == playingStep) {
                return StepPadLightHelper.renderPlayheadHighlight(
                        accented ? occupiedStepColor.getBrightend() : occupiedStepColor);
            }
            return StepPadLightHelper.renderOccupiedStep(occupiedStepColor, accented, false);
        }
        if (sustained) {
            return stepIndex == playingStep
                    ? StepPadLightHelper.renderPlayheadHighlight(sustainedStepColor)
                    : sustainedStepColor;
        }
        return StepPadLightHelper.renderEmptyStep(stepIndex, playingStep);
    }
}
