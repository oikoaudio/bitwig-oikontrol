package com.oikoaudio.fire.melodic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MelodicDensityEditorTest {

    @Test
    void thinningChoosesWeakestNonAnchorInsteadOfLatestStep() {
        final MelodicPattern pattern = patternWithActiveSteps(0, 3, 4, 8, 12, 14);

        assertEquals(3, MelodicDensityEditor.weakestRemovableStep(pattern).orElseThrow());
    }

    @Test
    void fillingRestoresStrongestMissingBaseStepInsteadOfEarliestStep() {
        final MelodicPattern base = patternWithActiveSteps(0, 3, 4, 8, 12, 14);
        final MelodicPattern current = patternWithActiveSteps(0, 4, 8, 12);

        assertEquals(14, MelodicDensityEditor.strongestRestorableStep(current, base).orElseThrow());
    }

    @Test
    void thinningProtectsStructuralQuarterNoteAnchorsWhileDecorationsRemain() {
        final MelodicPattern pattern = patternWithActiveSteps(0, 3, 4, 8, 12);

        assertEquals(3, MelodicDensityEditor.weakestRemovableStep(pattern).orElseThrow());
    }

    @Test
    void normalDensityCanRegenerateAnExistingPhraseWithoutARecordedGenerationSeed() {
        assertEquals(41L, MelodicDensityEditor.regenerationSeed(-1L, 41L, 6).orElseThrow());
        assertEquals(17L, MelodicDensityEditor.regenerationSeed(17L, 41L, 6).orElseThrow());
        assertTrue(MelodicDensityEditor.regenerationSeed(-1L, 41L, 0).isEmpty());
    }

    @Test
    void normalDensityFeedbackRemainsTheNormalizedParameterValue() {
        assertEquals("0.50", MelodicDensityEditor.parameterLabel(0.5));
        assertEquals("1.00", MelodicDensityEditor.parameterLabel(1.0));
    }

    private static MelodicPattern patternWithActiveSteps(final int... activeSteps) {
        final java.util.Set<Integer> active =
                java.util.Arrays.stream(activeSteps)
                        .boxed()
                        .collect(java.util.stream.Collectors.toSet());
        final List<MelodicPattern.Step> steps =
                java.util.stream.IntStream.range(0, MelodicPattern.MAX_STEPS)
                        .mapToObj(
                                index ->
                                        active.contains(index)
                                                ? new MelodicPattern.Step(
                                                        index,
                                                        true,
                                                        false,
                                                        60 + index,
                                                        96,
                                                        0.8,
                                                        false,
                                                        false)
                                                : MelodicPattern.Step.rest(index))
                        .toList();
        return new MelodicPattern(steps, 16);
    }
}
