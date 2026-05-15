package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.RecurrencePattern;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class MelodicStepPadSurface {
    static final int CLIP_ROW_PAD_COUNT = 16;
    static final int PITCH_POOL_PAD_OFFSET = 16;
    static final int PITCH_POOL_PAD_COUNT = 16;
    static final int STEP_PAD_OFFSET = 32;
    static final int STEP_COUNT = 32;
    private static final long RECURRENCE_LIGHT_HOLD_MS = 180L;

    private final Callbacks callbacks;
    private int selectedStep = 0;
    private Integer heldStep = null;
    private final LinkedHashSet<Integer> heldSteps = new LinkedHashSet<>();
    private boolean heldStepConsumed = false;
    private boolean recurrenceSpanAnchorHeld = false;
    private boolean recurrenceSpanGestureUsed = false;
    private long firstStepHeldAtMs = 0L;

    MelodicStepPadSurface(final Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    void handlePadPress(final int padIndex, final boolean pressed) {
        if (!heldSteps.isEmpty() && padIndex < CLIP_ROW_PAD_COUNT && handleRecurrencePadPress(padIndex, pressed)) {
            return;
        }
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            callbacks.handleClipRowPad(padIndex, pressed);
            return;
        }
        if (padIndex < STEP_PAD_OFFSET && !pressed) {
            callbacks.stopPitchPoolAudition(padIndex - PITCH_POOL_PAD_OFFSET);
            return;
        }
        if (padIndex >= STEP_PAD_OFFSET && !pressed) {
            releaseStepPad(padIndex - STEP_PAD_OFFSET);
            return;
        }
        if (!pressed) {
            return;
        }
        if (padIndex < STEP_PAD_OFFSET) {
            callbacks.togglePitchPoolPad(padIndex - PITCH_POOL_PAD_OFFSET, heldStep);
            if (heldStep != null) {
                heldStepConsumed = true;
            }
            return;
        }
        pressStepPad(padIndex - STEP_PAD_OFFSET);
    }

    RgbLigthState getPadLight(final int padIndex) {
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            if (shouldShowRecurrenceRow()) {
                return getRecurrencePadLight(padIndex);
            }
            return callbacks.clipRowPadLight(padIndex);
        }
        if (padIndex < STEP_PAD_OFFSET) {
            return callbacks.pitchPoolPadLight(padIndex - PITCH_POOL_PAD_OFFSET);
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        return MelodicRenderer.stepLight(callbacks.currentPattern().step(stepIndex), heldSteps.contains(stepIndex),
                stepIndex < callbacks.loopSteps(), stepIndex == callbacks.playingStep(), stepIndex,
                callbacks.selectedClipColor());
    }

    int editingStepIndex() {
        return heldStep != null ? heldStep : selectedStep;
    }

    int selectedStep() {
        return selectedStep;
    }

    void clampSelectedStep(final int loopSteps) {
        selectedStep = Math.min(selectedStep, Math.max(0, loopSteps - 1));
    }

    boolean hasHeldSteps() {
        return !heldSteps.isEmpty();
    }

    int heldStepCount() {
        return heldSteps.size();
    }

    List<Integer> heldSteps() {
        return List.copyOf(heldSteps);
    }

    void consumeHeldStepGesture() {
        if (!heldSteps.isEmpty()) {
            heldStepConsumed = true;
        }
    }

    List<Integer> heldRecurrenceTargets() {
        return heldActiveSteps();
    }

    List<Integer> heldEditableTargets() {
        return heldActiveSteps();
    }

    String detailsLabel() {
        return heldSteps.size() > 1 ? "%d steps".formatted(heldSteps.size()) : "Step " + (selectedStep + 1);
    }

    private void pressStepPad(final int stepIndex) {
        final boolean accentGesture = callbacks.isAccentGestureActive();
        if (heldSteps.isEmpty()) {
            firstStepHeldAtMs = System.currentTimeMillis();
        }
        heldSteps.add(stepIndex);
        heldStep = stepIndex;
        heldStepConsumed = heldSteps.size() > 1;
        if (accentGesture) {
            heldStepConsumed = true;
            if (callbacks.isAccentHeld()) {
                callbacks.markAccentModified();
            }
            callbacks.toggleAccent(stepIndex);
            return;
        }
        if (callbacks.isFixedLengthHeld()) {
            heldStepConsumed = true;
            callbacks.setLoopSteps(stepIndex + 1);
            return;
        }
        if (callbacks.isCopyHeld()) {
            heldStepConsumed = true;
            callbacks.showPasteClipRowOnly();
            return;
        }
        if (callbacks.isDeleteHeld()) {
            heldStepConsumed = true;
            callbacks.clearStep(stepIndex);
            return;
        }
        selectedStep = stepIndex;
        callbacks.showStepSelection(stepIndex);
    }

    private void releaseStepPad(final int stepIndex) {
        final boolean accentGesture = callbacks.isAccentGestureActive();
        if (heldSteps.remove(stepIndex)) {
            if (!heldStepConsumed && heldSteps.isEmpty() && !accentGesture
                    && !callbacks.isFixedLengthHeld() && !callbacks.isDeleteHeld()) {
                callbacks.toggleStep(stepIndex);
            }
            heldStep = heldSteps.isEmpty() ? null : heldSteps.iterator().next();
            if (heldSteps.isEmpty()) {
                heldStepConsumed = false;
                recurrenceSpanAnchorHeld = false;
                recurrenceSpanGestureUsed = false;
                firstStepHeldAtMs = 0L;
            }
        }
    }

    private boolean shouldShowRecurrenceRow() {
        return !heldSteps.isEmpty()
                && firstStepHeldAtMs > 0L
                && System.currentTimeMillis() - firstStepHeldAtMs >= RECURRENCE_LIGHT_HOLD_MS;
    }

    private boolean handleRecurrencePadPress(final int padIndex, final boolean pressed) {
        if (heldSteps.isEmpty() || padIndex >= 8) {
            return false;
        }
        if (padIndex == 0 && !pressed && recurrenceSpanAnchorHeld) {
            recurrenceSpanAnchorHeld = false;
            if (recurrenceSpanGestureUsed) {
                recurrenceSpanGestureUsed = false;
                return true;
            }
        } else if (!pressed) {
            return true;
        }
        final List<Integer> targets = heldRecurrenceTargets();
        if (targets.isEmpty()) {
            return true;
        }
        heldStepConsumed = true;
        if (padIndex == 0) {
            recurrenceSpanAnchorHeld = true;
            recurrenceSpanGestureUsed = false;
            return true;
        }
        if (recurrenceSpanAnchorHeld) {
            recurrenceSpanGestureUsed = true;
            callbacks.applyHeldRecurrenceSpan(targets, padIndex + 1);
            return true;
        }
        final MelodicPattern.Step step = callbacks.currentPattern().step(targets.get(0));
        final RecurrencePattern recurrence = recurrenceOf(step);
        final int span = recurrence.effectiveSpan();
        if (padIndex >= span) {
            return true;
        }
        callbacks.applyHeldRecurrenceToggle(targets, padIndex);
        return true;
    }

    private RgbLigthState getRecurrencePadLight(final int padIndex) {
        final List<Integer> targets = heldRecurrenceTargets();
        if (targets.isEmpty() || padIndex >= 8) {
            return RgbLigthState.OFF;
        }
        final MelodicPattern.Step step = callbacks.currentPattern().step(targets.get(0));
        if (!step.active() || step.pitch() == null) {
            return RgbLigthState.OFF;
        }
        final RecurrencePattern recurrence = recurrenceOf(step);
        final int span = recurrence.effectiveSpan();
        if (padIndex >= span) {
            return RgbLigthState.OFF;
        }
        final int mask = recurrence.effectiveMask(span);
        return ((mask >> padIndex) & 0x1) == 1
                ? callbacks.selectedClipColor().getBrightend()
                : callbacks.selectedClipColor().getDimmed();
    }

    private List<Integer> heldActiveSteps() {
        final List<Integer> targets = new ArrayList<>();
        for (final int stepIndex : heldSteps) {
            if (stepIndex < 0 || stepIndex >= STEP_COUNT) {
                continue;
            }
            final MelodicPattern.Step step = callbacks.currentPattern().step(stepIndex);
            if (step.active() && step.pitch() != null) {
                targets.add(stepIndex);
            }
        }
        return targets;
    }

    private static RecurrencePattern recurrenceOf(final MelodicPattern.Step step) {
        return RecurrencePattern.of(step.recurrenceLength(), step.recurrenceMask());
    }

    interface Callbacks {
        boolean isAccentGestureActive();

        boolean isAccentHeld();

        void markAccentModified();

        boolean isFixedLengthHeld();

        boolean isCopyHeld();

        boolean isDeleteHeld();

        void handleClipRowPad(int padIndex, boolean pressed);

        RgbLigthState clipRowPadLight(int padIndex);

        void stopPitchPoolAudition(int poolPadIndex);

        void togglePitchPoolPad(int poolPadIndex, Integer heldStep);

        RgbLigthState pitchPoolPadLight(int poolPadIndex);

        void toggleStep(int stepIndex);

        void toggleAccent(int stepIndex);

        void setLoopSteps(int loopSteps);

        void showPasteClipRowOnly();

        void clearStep(int stepIndex);

        void showStepSelection(int stepIndex);

        MelodicPattern currentPattern();

        int loopSteps();

        int playingStep();

        RgbLigthState selectedClipColor();

        void applyHeldRecurrenceSpan(List<Integer> stepIndices, int newSpan);

        void applyHeldRecurrenceToggle(List<Integer> stepIndices, int padIndex);
    }
}
