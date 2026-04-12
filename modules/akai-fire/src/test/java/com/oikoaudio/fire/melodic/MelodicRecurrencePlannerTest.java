package com.oikoaudio.fire.melodic;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MelodicRecurrencePlannerTest {

    @Test
    void zeroTimeVarianceClearsRecurrence() {
        final MelodicPattern base = patternWithNotes(0, 2, 4, 6);
        final MelodicPattern withRecurrence = base.withStep(base.step(2).withRecurrence(4, 0b0101));

        final MelodicPattern out = MelodicRecurrencePlanner.apply(
                withRecurrence, testContext(), MelodicRecurrencePlanner.Style.MOTIF, 0.0, 42L);

        for (int i = 0; i < out.loopSteps(); i++) {
            assertEquals(0, out.step(i).recurrenceLength());
            assertEquals(0, out.step(i).recurrenceMask());
        }
    }

    @Test
    void higherTimeVarianceAssignsRecurrenceBeyondBoundaryAnchors() {
        final MelodicPattern base = patternWithNotes(0, 1, 2, 3, 4, 6, 7, 10, 11, 14);

        final MelodicPattern out = MelodicRecurrencePlanner.apply(
                base, testContext(), MelodicRecurrencePlanner.Style.CALL_RESPONSE, 0.75, 7L);

        assertEquals(0, out.step(0).recurrenceLength());
        assertTrue(out.steps().stream().anyMatch(step -> step.recurrenceLength() > 1));
        assertTrue(out.steps().stream().anyMatch(step ->
                step.index() != 0
                        && step.index() != out.loopSteps() - 1
                        && step.recurrenceLength() > 1));
    }

    @Test
    void callResponseBiasesVariationIntoResponseHalf() {
        final MelodicPattern base = patternWithNotes(0, 2, 3, 4, 6, 7, 8, 10, 11, 12, 14, 15);

        final MelodicPattern out = MelodicRecurrencePlanner.apply(
                base, testContext(), MelodicRecurrencePlanner.Style.CALL_RESPONSE, 0.8, 11L);

        final long variedInResponse = countRecurrent(out, 8, 16);
        final long variedInCall = countRecurrent(out, 0, 8);
        assertTrue(variedInResponse >= variedInCall);
    }

    @Test
    void highVarianceCanInjectRecurrentOrnament() {
        final MelodicPattern base = patternWithNotes(0, 2, 4, 6, 8, 10, 12, 14);

        final MelodicPattern out = MelodicRecurrencePlanner.apply(
                base, testContext(), MelodicRecurrencePlanner.Style.ACID, 0.95, 17L);

        assertTrue(out.steps().stream().anyMatch(step ->
                step.active() && !base.step(step.index()).active() && step.recurrenceLength() > 1));
    }

    @Test
    void acidBiasesRecurrenceTowardMidAndLateFigures() {
        final MelodicPattern base = patternWithNotes(0, 2, 4, 5, 6, 8, 10, 12, 13, 14);

        final MelodicPattern out = MelodicRecurrencePlanner.apply(
                base, testContext(), MelodicRecurrencePlanner.Style.ACID, 0.85, 19L);

        assertEquals(0, out.step(2).recurrenceLength());
        assertTrue(countRecurrent(out, 4, 16) >= countRecurrent(out, 0, 4));
    }

    @Test
    void acidUsesLighterRecurrenceOnThirtyTwoStepPhrases() {
        final MelodicPattern base = patternWithLoopAndNotes(32,
                0, 2, 4, 5, 6, 8, 10, 12, 13, 14, 16, 18, 20, 21, 22, 24, 26, 28, 29, 30);

        final MelodicPattern out = MelodicRecurrencePlanner.apply(
                base, testContext(), MelodicRecurrencePlanner.Style.ACID, 0.9, 23L);

        assertTrue(countRecurrent(out, 0, 32) <= 6);
    }

    private static MelodicPattern patternWithNotes(final int... activeSteps) {
        return patternWithLoopAndNotes(16, activeSteps);
    }

    private static MelodicPattern patternWithLoopAndNotes(final int loopSteps, final int... activeSteps) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        for (final int stepIndex : activeSteps) {
            steps.set(stepIndex, new MelodicPattern.Step(stepIndex, true, false, 48 + stepIndex, 96,
                    0.8, stepIndex % 4 == 0, false));
        }
        return new MelodicPattern(steps, loopSteps);
    }

    private static MelodicPhraseContext testContext() {
        return new MelodicPhraseContext(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                48);
    }

    private static long countRecurrent(final MelodicPattern pattern, final int startInclusive, final int endExclusive) {
        long count = 0;
        for (int i = startInclusive; i < endExclusive; i++) {
            if (pattern.step(i).recurrenceLength() > 1) {
                count++;
            }
        }
        return count;
    }
}
