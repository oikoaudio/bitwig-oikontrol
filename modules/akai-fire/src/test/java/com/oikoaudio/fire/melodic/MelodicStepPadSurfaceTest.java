package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.lights.RgbLigthState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MelodicStepPadSurfaceTest {
    @Test
    void stepPressAndReleaseTogglesTheStep() {
        final FakeCallbacks callbacks = new FakeCallbacks();
        final MelodicStepPadSurface surface = new MelodicStepPadSurface(callbacks);

        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 4, true);
        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 4, false);

        assertEquals(List.of(4), callbacks.toggledSteps);
    }

    @Test
    void pitchPoolPressWhileHoldingStepAssignsPitchWithoutTogglingStepOnRelease() {
        final FakeCallbacks callbacks = new FakeCallbacks();
        final MelodicStepPadSurface surface = new MelodicStepPadSurface(callbacks);

        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 6, true);
        surface.handlePadPress(MelodicStepPadSurface.PITCH_POOL_PAD_OFFSET + 3, true);
        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 6, false);

        assertEquals(3, callbacks.lastPoolPad);
        assertEquals(6, callbacks.lastPoolHeldStep);
        assertEquals(List.of(), callbacks.toggledSteps);
    }

    @Test
    void consumedHeldStepGestureDoesNotToggleStepOnRelease() {
        final FakeCallbacks callbacks = new FakeCallbacks();
        final MelodicStepPadSurface surface = new MelodicStepPadSurface(callbacks);

        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 6, true);
        surface.consumeHeldStepGesture();
        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 6, false);

        assertEquals(List.of(), callbacks.toggledSteps);
    }

    @Test
    void heldStepsTurnClipRowPadsIntoRecurrenceControls() {
        final FakeCallbacks callbacks = new FakeCallbacks();
        callbacks.pattern = callbacks.pattern
                .withStep(activeStep(2, 60))
                .withStep(activeStep(5, 64));
        final MelodicStepPadSurface surface = new MelodicStepPadSurface(callbacks);

        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 2, true);
        surface.handlePadPress(MelodicStepPadSurface.STEP_PAD_OFFSET + 5, true);
        surface.handlePadPress(1, true);

        assertEquals(List.of(2, 5), callbacks.lastRecurrenceToggleTargets);
        assertEquals(1, callbacks.lastRecurrenceTogglePad);
        assertNull(callbacks.lastClipPad);
    }

    private static MelodicPattern.Step activeStep(final int index, final int pitch) {
        return new MelodicPattern.Step(index, true, false, pitch, 96, 0.8, false, false);
    }

    private static final class FakeCallbacks implements MelodicStepPadSurface.Callbacks {
        private final List<Integer> toggledSteps = new ArrayList<>();
        private MelodicPattern pattern = MelodicPattern.empty(16);
        private Integer lastPoolPad = null;
        private Integer lastPoolHeldStep = null;
        private Integer lastClipPad = null;
        private List<Integer> lastRecurrenceToggleTargets = List.of();
        private int lastRecurrenceTogglePad = -1;

        @Override
        public boolean isAccentGestureActive() {
            return false;
        }

        @Override
        public boolean isAccentHeld() {
            return false;
        }

        @Override
        public void markAccentModified() {
        }

        @Override
        public boolean isFixedLengthHeld() {
            return false;
        }

        @Override
        public boolean isCopyHeld() {
            return false;
        }

        @Override
        public boolean isDeleteHeld() {
            return false;
        }

        @Override
        public void handleClipRowPad(final int padIndex, final boolean pressed) {
            lastClipPad = padIndex;
        }

        @Override
        public RgbLigthState clipRowPadLight(final int padIndex) {
            return RgbLigthState.OFF;
        }

        @Override
        public void stopPitchPoolAudition(final int poolPadIndex) {
        }

        @Override
        public void togglePitchPoolPad(final int poolPadIndex, final Integer heldStep) {
            lastPoolPad = poolPadIndex;
            lastPoolHeldStep = heldStep;
        }

        @Override
        public RgbLigthState pitchPoolPadLight(final int poolPadIndex) {
            return RgbLigthState.OFF;
        }

        @Override
        public void toggleStep(final int stepIndex) {
            toggledSteps.add(stepIndex);
        }

        @Override
        public void toggleAccent(final int stepIndex) {
        }

        @Override
        public void setLoopSteps(final int loopSteps) {
        }

        @Override
        public void showPasteClipRowOnly() {
        }

        @Override
        public void clearStep(final int stepIndex) {
        }

        @Override
        public void showStepSelection(final int stepIndex) {
        }

        @Override
        public MelodicPattern currentPattern() {
            return pattern;
        }

        @Override
        public int loopSteps() {
            return pattern.loopSteps();
        }

        @Override
        public int playingStep() {
            return -1;
        }

        @Override
        public RgbLigthState selectedClipColor() {
            return MelodicRenderer.ACTIVE_STEP;
        }

        @Override
        public void applyHeldRecurrenceSpan(final List<Integer> stepIndices, final int newSpan) {
        }

        @Override
        public void applyHeldRecurrenceToggle(final List<Integer> stepIndices, final int padIndex) {
            lastRecurrenceToggleTargets = List.copyOf(stepIndices);
            lastRecurrenceTogglePad = padIndex;
        }
    }
}
