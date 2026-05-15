package com.oikoaudio.fire.melodic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MelodicStepPatternStateTest {
    @Test
    void observedEmptyStepKeepsLatentPitchAsInactiveRestorableStep() {
        final MelodicStepPatternState state = new MelodicStepPatternState(16);
        state.setCurrentPattern(MelodicPattern.empty(16).withStep(activeStep(4, 64)));
        state.setBasePattern(MelodicPattern.empty(16).withStep(activeStep(5, 67)));

        state.applyObservedPattern(MelodicPattern.empty(8));

        assertFalse(state.currentPattern().step(4).active());
        assertEquals(64, state.currentPattern().step(4).pitch());
        assertTrue(state.basePattern().step(5).active());
        assertEquals(67, state.basePattern().step(5).pitch());
        assertEquals(8, state.loopSteps());
    }

    @Test
    void observedEmptyStepDoesNotRemoveBaseStepNeededForFill() {
        final MelodicStepPatternState state = new MelodicStepPatternState(16);
        state.setCurrentPattern(MelodicPattern.empty(16));
        state.setBasePattern(MelodicPattern.empty(16).withStep(activeStep(5, 67)));

        state.applyObservedPattern(MelodicPattern.empty(16));

        final MelodicPattern.Step restored = state.restoreGeneratedStepOrDefault(5, () -> activeStep(5, 60));
        assertTrue(restored.active());
        assertEquals(67, restored.pitch());
    }

    @Test
    void observedNoteReplacesLatentStep() {
        final MelodicStepPatternState state = new MelodicStepPatternState(16);
        state.setCurrentPattern(MelodicPattern.empty(16).withStep(activeStep(3, 60)));

        state.applyObservedPattern(MelodicPattern.empty(16).withStep(activeStep(3, 72)));

        assertTrue(state.currentPattern().step(3).active());
        assertEquals(72, state.currentPattern().step(3).pitch());
    }

    @Test
    void ensureStepCreatesAndStoresDefaultOnlyForInactiveSteps() {
        final MelodicStepPatternState state = new MelodicStepPatternState(16);
        final MelodicPattern.Step created = activeStep(2, 65);

        assertSame(created, state.ensureStep(2, () -> created));
        assertEquals(created, state.currentPattern().step(2));

        final MelodicPattern.Step active = activeStep(3, 70);
        state.setCurrentPattern(state.currentPattern().withStep(active));

        assertEquals(active, state.ensureStep(3, () -> activeStep(3, 71)));
    }

    @Test
    void restoreGeneratedStepFallsBackWhenBaseHasNoPitch() {
        final MelodicStepPatternState state = new MelodicStepPatternState(16);
        final MelodicPattern.Step fallback = activeStep(6, 69);

        assertSame(fallback, state.restoreGeneratedStepOrDefault(6, () -> fallback));
    }

    @Test
    void restoreGeneratedStepUsesActiveBasePitch() {
        final MelodicStepPatternState state = new MelodicStepPatternState(16);
        state.setBasePattern(MelodicPattern.empty(16).withStep(activeStep(7, 74)));

        final MelodicPattern.Step restored = state.restoreGeneratedStepOrDefault(7, () -> activeStep(7, 60));

        assertTrue(restored.active());
        assertEquals(74, restored.pitch());
    }

    private static MelodicPattern.Step activeStep(final int index, final int pitch) {
        return new MelodicPattern.Step(index, true, false, pitch, 96, 0.8, false, false);
    }
}
