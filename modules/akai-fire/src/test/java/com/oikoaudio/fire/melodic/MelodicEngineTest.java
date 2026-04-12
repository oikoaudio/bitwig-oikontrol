package com.oikoaudio.fire.melodic;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MelodicEngineTest {

    @Test
    void motifGeneratorIsDeterministicForSeed() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.45, 0.25, 0.1, 0.2, 5, 0, 0.0, 23L);

        final MelodicPattern a = new MotifGenerator().generate(context, parameters);
        final MelodicPattern b = new MotifGenerator().generate(context, parameters);

        assertArrayEquals(activeMask(a), activeMask(b));
        assertArrayEquals(pitchMask(a), pitchMask(b));
    }

    @Test
    void euclideanGeneratorUsesExpectedPulseCount() {
        final boolean[] pattern = EuclideanPhraseGenerator.euclidean(16, 5, 2);
        int count = 0;
        for (final boolean step : pattern) {
            if (step) {
                count++;
            }
        }
        assertEquals(5, count);
        assertTrue(pattern[2]);
    }

    @Test
    void preserveRhythmMutationKeepsActiveMask() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.55, 0.35, 0.2, 0.2, 5, 0, 0.0, 9L);
        final MelodicPattern original = new MotifGenerator().generate(context, parameters);

        final MelodicPattern mutated = new MelodicMutator().mutate(original, context,
                MelodicRecurrencePlanner.Style.MOTIF, MelodicMutator.Mode.PRESERVE_RHYTHM, 0.8, 0.6, 77L);

        assertArrayEquals(activeMask(original), activeMask(mutated));
    }

    @Test
    void simplifyAndDensifyMoveDensityInExpectedDirection() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.4, 0.2, 0.1, 0.2, 5, 0, 0.0, 12L);
        final MelodicPattern original = new MotifGenerator().generate(context, parameters);
        final MelodicMutator mutator = new MelodicMutator();

        final MelodicPattern simplified = mutator.mutate(original, context,
                MelodicRecurrencePlanner.Style.MOTIF, MelodicMutator.Mode.SIMPLIFY, 0.9, 0.1, 4L);
        final MelodicPattern densified = mutator.mutate(original, context,
                MelodicRecurrencePlanner.Style.MOTIF, MelodicMutator.Mode.DENSIFY, 0.9, 0.1, 4L);

        assertTrue(activeCount(simplified) <= activeCount(original));
        assertTrue(activeCount(densified) >= activeCount(original));
    }

    @Test
    void preserveRhythmMutationActuallyChangesPitchMaterial() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.55, 0.35, 0.2, 0.2, 5, 0, 0.0, 9L);
        final MelodicPattern original = new MotifGenerator().generate(context, parameters);

        final MelodicPattern mutated = new MelodicMutator().mutate(original, context,
                MelodicRecurrencePlanner.Style.MOTIF, MelodicMutator.Mode.PRESERVE_RHYTHM, 0.8, 0.6, 77L);

        assertNotEquals(java.util.Arrays.toString(pitchMask(original)), java.util.Arrays.toString(pitchMask(mutated)));
    }

    @Test
    void varyTimeMutationAddsRecurrenceWithoutRewritingTheVisibleMotif() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.5, 0.3, 0.6, 0.2, 5, 0, 0.0, 41L);
        final MelodicPattern original = new CallResponseGenerator().generate(context, parameters);

        final MelodicPattern mutated = new MelodicMutator().mutate(original, context,
                MelodicRecurrencePlanner.Style.CALL_RESPONSE, MelodicMutator.Mode.VARY_TIME, 0.8, 0.6, 77L);

        assertTrue(activeCount(mutated) >= activeCount(original));
        assertTrue(activeCount(mutated) <= activeCount(original) + 1);
        assertTrue(mutated.steps().stream().anyMatch(step -> step.recurrenceLength() > 1));
    }

    @Test
    void varyTimeMutationDoesNotGetStuckOnRepeatedPasses() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(32, 0.5, 0.3, 0.6, 0.2, 5, 0, 0.0, 41L);
        final MelodicMutator mutator = new MelodicMutator();
        MelodicPattern current = new CallResponseGenerator().generate(context, parameters).withLoopSteps(32);

        boolean changed = false;
        for (int i = 0; i < 6; i++) {
            final MelodicPattern next = mutator.mutate(current, context,
                    MelodicRecurrencePlanner.Style.CALL_RESPONSE, MelodicMutator.Mode.VARY_TIME, 1.0, 0.6, 77L + i);
            if (!next.equals(current)) {
                changed = true;
            }
            current = next;
        }

        assertTrue(changed);
    }

    @Test
    void clipAdapterAlwaysBuildsExactlyThirtyTwoSteps() {
        final Map<Integer, Map<Integer, com.bitwig.extension.controller.api.NoteStep>> map = new HashMap<>();
        final MelodicPattern pattern = MelodicClipAdapter.fromNoteSteps(map, 16, 0.25);
        assertEquals(32, pattern.steps().size());
    }

    @Test
    void rollingGeneratorCreatesDenseConnectedPhrase() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.7, 0.25, 0.15, 0.2, 6, 0, 0.0, 31L);

        final MelodicPattern pattern = new RollingBassGenerator().generate(context, parameters);

        assertTrue(activeCount(pattern) >= 12);
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (pattern.step(i).active()) {
                assertTrue(pattern.step(i).gate() >= 0.95);
            }
        }
    }

    @Test
    void rollingGeneratorUsesAtLeastThreePitches() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.72, 0.35, 1.0, 0.2, 6, 0, 0.0, 31L);

        final MelodicPattern pattern = new RollingBassGenerator().generate(context, parameters);

        assertTrue(distictActivePitchCount(pattern) >= 3);
    }

    @Test
    void collapsedPitchPoolProducesThirtyTwoScaleNotes() {
        final MelodicPhraseContext context = context();

        final java.util.List<Integer> pool = context.collapsedScaleNotes(32);

        assertEquals(32, pool.size());
        for (final int midiNote : pool) {
            assertTrue(context.scale().isMidiNoteInScale(context.rootNote(), midiNote));
        }
    }

    @Test
    void acidGeneratorLeavesPhraseSpace() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.45, 0.6, 0.15, 0.2, 5, 0, 0.0, 17L);

        final MelodicPattern pattern = new AcidGenerator().generate(context, parameters);

        assertTrue(activeCount(pattern) <= 12);
        assertTrue(activeCount(pattern) >= 4);
    }

    @Test
    void acidGeneratorDensityHasRealRange() {
        final MelodicPhraseContext context = context();
        final MelodicPattern sparse = new AcidGenerator().generate(context,
                new MelodicGenerator.GenerateParameters(16, 0.0, 0.6, 0.15, 0.2, 5, 0, 0.0, 17L));
        final MelodicPattern dense = new AcidGenerator().generate(context,
                new MelodicGenerator.GenerateParameters(16, 1.0, 0.6, 0.15, 0.2, 5, 0, 0.0, 17L));

        assertTrue(activeCount(sparse) <= 8);
        assertTrue(activeCount(dense) >= 11);
        assertTrue(activeCount(dense) >= activeCount(sparse) + 3);
    }

    @Test
    void acidGeneratorUsesMoreThanOnePitch() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.52, 0.62, 1.0, 0.2, 5, 0, 0.0, 17L);

        final MelodicPattern pattern = new AcidGenerator().generate(context, parameters);

        assertTrue(distictActivePitchCount(pattern) >= 3);
    }

    @Test
    void acidVaryTimeMutationIntroducesRecurrenceOrAlternates() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(32, 0.55, 0.62, 0.3, 0.36, 5, 0, 0.0, 17L);
        final MelodicPattern original = new AcidGenerator().generate(context, parameters).withLoopSteps(32);

        final MelodicPattern mutated = new MelodicMutator().mutate(original, context,
                MelodicRecurrencePlanner.Style.ACID, MelodicMutator.Mode.VARY_TIME, 1.0, 0.6, 77L);

        assertNotEquals(original, mutated);
        assertTrue(hasRecurrenceOrAlternate(mutated));
        assertTrue(recurringCarrierCount(mutated) >= 3);
    }

    @Test
    void acidDefaultStrengthVaryTimeMutationStillDoesSomething() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.5, 0.62, 0.15, 0.36, 5, 0, 0.0, 17L);
        final MelodicPattern original = new AcidGenerator().generate(context, parameters);

        final MelodicPattern mutated = new MelodicMutator().mutate(original, context,
                MelodicRecurrencePlanner.Style.ACID, MelodicMutator.Mode.VARY_TIME, 0.45, 0.7, 17L);

        assertNotEquals(original, mutated);
        assertTrue(hasRecurrenceOrAlternate(mutated));
        assertTrue(recurringCarrierCount(mutated) >= 2);
    }

    @Test
    void callResponseGeneratorAnswersTheCall() {
        final MelodicPhraseContext context = context();
        final MelodicGenerator.GenerateParameters parameters =
                new MelodicGenerator.GenerateParameters(16, 0.5, 0.3, 0.6, 0.2, 5, 0, 0.0, 41L);

        final MelodicPattern pattern = new CallResponseGenerator().generate(context, parameters);

        assertTrue(activeCount(pattern) >= 6);
        int mirroredHits = 0;
        for (int i = 0; i < 8; i++) {
            if (pattern.step(i).active() && pattern.step(i + 8).active()) {
                mirroredHits++;
            }
        }
        assertTrue(mirroredHits >= 3);
    }

    @Test
    void generatorsWithFamiliesExposeAnyAndNamedSubtypeCycling() {
        final MelodicGenerator[] generators = {
                new AcidGenerator(),
                new MotifGenerator(),
                new CallResponseGenerator(),
                new RollingBassGenerator()
        };

        for (final MelodicGenerator generator : generators) {
            assertTrue(generator.supportsSubtypeSelection());
            assertEquals("Any", generator.currentSubtypeLabel());
            generator.cycleSubtype(1);
            assertNotEquals("Any", generator.currentSubtypeLabel());
            generator.cycleSubtype(-1);
            assertEquals("Any", generator.currentSubtypeLabel());
        }
    }

    private MelodicPhraseContext context() {
        return new MelodicPhraseContext(
                MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"),
                0,
                36);
    }

    private boolean[] activeMask(final MelodicPattern pattern) {
        final boolean[] mask = new boolean[pattern.loopSteps()];
        for (int i = 0; i < pattern.loopSteps(); i++) {
            mask[i] = pattern.step(i).active();
        }
        return mask;
    }

    private int[] pitchMask(final MelodicPattern pattern) {
        final int[] mask = new int[pattern.loopSteps()];
        for (int i = 0; i < pattern.loopSteps(); i++) {
            mask[i] = pattern.step(i).pitch() == null ? -1 : pattern.step(i).pitch();
        }
        return mask;
    }

    private int activeCount(final MelodicPattern pattern) {
        int count = 0;
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (pattern.step(i).active()) {
                count++;
            }
        }
        return count;
    }

    private int distictActivePitchCount(final MelodicPattern pattern) {
        final Set<Integer> pitches = new HashSet<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.active() && step.pitch() != null) {
                pitches.add(step.pitch());
            }
        }
        return pitches.size();
    }

    private boolean hasRecurrenceOrAlternate(final MelodicPattern pattern) {
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.recurrenceLength() > 1 || step.hasAlternate()) {
                return true;
            }
        }
        return false;
    }

    private int recurringCarrierCount(final MelodicPattern pattern) {
        int count = 0;
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (pattern.step(i).recurrenceLength() > 1) {
                count++;
            }
        }
        return count;
    }
}
