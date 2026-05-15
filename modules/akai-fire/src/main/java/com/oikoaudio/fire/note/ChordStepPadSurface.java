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

    interface StepPadCallbacks {
        boolean ensureSelectedNoteClipSlot();

        boolean isAccentGestureActive();

        void toggleAccentForStep(int stepIndex);

        boolean isSelectHeld();

        boolean isFixedLengthHeld();

        boolean isCopyHeld();

        boolean isDeleteHeld();

        void handleSelectStep(int stepIndex);

        void setLastStep(int stepIndex);

        void pasteCurrentChordToStep(int stepIndex);

        void clearChordStep(int stepIndex);

        boolean isBuilderFamily();

        boolean canExtendHeldChordRange(int anchorStepIndex, int targetStepIndex);

        boolean extendHeldChordRange(int anchorStepIndex, int targetStepIndex);

        void showExtendedStepInfo(int anchorStepIndex, int targetStepIndex);

        void showBlockedStepInfo();

        boolean hasStepStartNote(int stepIndex);

        boolean assignSelectedChordToStep(int stepIndex, int velocity);

        void loadBuilderFromStep(int stepIndex);

        void showHeldStepInfo(int stepIndex);

        void removeHeldBankFineStart(int stepIndex);
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

    void handleStepPadPress(final int stepIndex,
                            final boolean pressed,
                            final int velocity,
                            final StepPadCallbacks callbacks) {
        if (pressed && !callbacks.ensureSelectedNoteClipSlot()) {
            return;
        }
        if (callbacks.isAccentGestureActive()) {
            if (pressed) {
                callbacks.toggleAccentForStep(stepIndex);
            }
            return;
        }
        if (!pressed && consumeModifierHandledStep(stepIndex)) {
            return;
        }
        if (pressed) {
            handleStepPadPressed(stepIndex, velocity, callbacks);
        } else {
            handleStepPadReleased(stepIndex, callbacks);
        }
    }

    private void handleStepPadPressed(final int stepIndex,
                                      final int velocity,
                                      final StepPadCallbacks callbacks) {
        final ModifierPressAction modifierAction = modifierPressAction(
                callbacks.isSelectHeld(), callbacks.isFixedLengthHeld(), callbacks.isCopyHeld(),
                callbacks.isDeleteHeld());
        if (handleModifierPressAction(stepIndex, modifierAction, callbacks)) {
            return;
        }
        final Integer anchor = heldStepAnchor;
        final boolean canExtendFromAnchor = anchor != null
                && anchor != stepIndex
                && hasHeldStep(anchor)
                && callbacks.canExtendHeldChordRange(anchor, stepIndex);
        final RangePressAction rangeAction = rangePressAction(stepIndex, canExtendFromAnchor);
        if (handleRangePressAction(stepIndex, anchor, rangeAction, callbacks)) {
            return;
        }
        final StepPressAction stepAction = stepPressAction(stepIndex, callbacks.hasStepStartNote(stepIndex),
                callbacks.isBuilderFamily());
        if (!handleNormalPressAction(stepIndex, velocity, stepAction, callbacks)) {
            return;
        }
        callbacks.showHeldStepInfo(stepIndex);
    }

    private void handleStepPadReleased(final int stepIndex, final StepPadCallbacks callbacks) {
        final StepReleaseAction releaseAction = stepReleaseAction(stepIndex, callbacks.hasStepStartNote(stepIndex));
        callbacks.removeHeldBankFineStart(stepIndex);
        if (releaseAction == StepReleaseAction.CLEAR_STEP) {
            callbacks.clearChordStep(stepIndex);
        }
    }

    private boolean handleModifierPressAction(final int stepIndex,
                                              final ModifierPressAction action,
                                              final StepPadCallbacks callbacks) {
        switch (action) {
            case NONE -> {
                return false;
            }
            case SELECT -> callbacks.handleSelectStep(stepIndex);
            case FIXED_LENGTH -> callbacks.setLastStep(stepIndex);
            case COPY -> callbacks.pasteCurrentChordToStep(stepIndex);
            case DELETE -> callbacks.clearChordStep(stepIndex);
        }
        markModifierHandledStep(stepIndex);
        return true;
    }

    private boolean handleRangePressAction(final int stepIndex,
                                           final Integer anchor,
                                           final RangePressAction action,
                                           final StepPadCallbacks callbacks) {
        switch (action) {
            case NONE -> {
                return false;
            }
            case EXTEND -> {
                if (anchor == null || !callbacks.extendHeldChordRange(anchor, stepIndex)) {
                    return false;
                }
                markRangeExtended(stepIndex);
                callbacks.showExtendedStepInfo(anchor, stepIndex);
                return true;
            }
            case BLOCK -> {
                callbacks.showBlockedStepInfo();
                return true;
            }
        }
        return false;
    }

    private boolean handleNormalPressAction(final int stepIndex,
                                            final int velocity,
                                            final StepPressAction action,
                                            final StepPadCallbacks callbacks) {
        switch (action) {
            case HOLD_EXISTING -> {
                return true;
            }
            case ADD_STEP -> {
                final boolean assigned = callbacks.assignSelectedChordToStep(stepIndex, velocity);
                if (!assigned) {
                    cancelStepPress(stepIndex);
                    return false;
                }
                markAddedStep(stepIndex);
                return true;
            }
            case LOAD_BUILDER -> {
                callbacks.loadBuilderFromStep(stepIndex);
                return true;
            }
        }
        return true;
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
