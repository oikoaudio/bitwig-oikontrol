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

        final MelodicPattern out = MelodicRecurrencePlanner.apply(withRecurrence, testContext(), 0.0, 42L);

        for (int i = 0; i < out.loopSteps(); i++) {
            assertEquals(0, out.step(i).recurrenceLength());
            assertEquals(0, out.step(i).recurrenceMask());
        }
    }

    @Test
    void higherTimeVarianceAssignsRecurrenceToNonAnchors() {
        final MelodicPattern base = patternWithNotes(0, 1, 2, 3, 4, 6, 7, 10, 11, 14);

        final MelodicPattern out = MelodicRecurrencePlanner.apply(base, testContext(), 0.75, 7L);

        assertEquals(0, out.step(0).recurrenceLength());
        assertEquals(0, out.step(4).recurrenceLength());
        assertTrue(out.steps().stream().anyMatch(step -> step.recurrenceLength() > 1));
    }

    private static MelodicPattern patternWithNotes(final int... activeSteps) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        for (final int stepIndex : activeSteps) {
            steps.set(stepIndex, new MelodicPattern.Step(stepIndex, true, false, 48 + stepIndex, 96,
                    0.8, stepIndex % 4 == 0, false));
        }
        return new MelodicPattern(steps, 16);
    }

    private static MelodicPhraseContext testContext() {
        return new MelodicPhraseContext(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                48);
    }
}
