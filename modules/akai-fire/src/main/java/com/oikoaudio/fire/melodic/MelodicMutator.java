package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class MelodicMutator {
    public enum Mode {
        PRESERVE_RHYTHM,
        VARY_ENDING,
        SIMPLIFY,
        DENSIFY
    }

    public MelodicPattern mutate(final MelodicPattern pattern, final MelodicPhraseContext context,
                                 final Mode mode, final double intensity, final double identityPreserve,
                                 final long seed) {
        return switch (mode) {
            case PRESERVE_RHYTHM -> preserveRhythm(pattern, context, intensity, identityPreserve, seed);
            case VARY_ENDING -> varyEnding(pattern, context, intensity, seed);
            case SIMPLIFY -> simplify(pattern, intensity, identityPreserve, seed);
            case DENSIFY -> densify(pattern, context, intensity, seed);
        };
    }

    private MelodicPattern preserveRhythm(final MelodicPattern pattern, final MelodicPhraseContext context,
                                          final double intensity, final double identityPreserve, final long seed) {
        final Random random = new Random(seed);
        final MelodicPatternAnalyzer.Analysis analysis = MelodicPatternAnalyzer.analyze(pattern);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        final List<Integer> candidates = new ArrayList<>();
        for (final int stepIndex : analysis.activeSteps()) {
            if (!analysis.anchorSteps().contains(stepIndex) || random.nextDouble() >= identityPreserve) {
                candidates.add(stepIndex);
            }
        }
        Collections.shuffle(candidates, random);
        boolean changed = false;
        for (final int stepIndex : candidates.subList(0, Math.min(changeBudget(intensity), candidates.size()))) {
            final MelodicPattern.Step step = steps.get(stepIndex);
            final int degree = random.nextInt(4 + (int) Math.round(intensity * 3));
            final MelodicPattern.Step updated = step.withPitch(context.pitchForDegree(0, degree));
            if (!updated.equals(step)) {
                changed = true;
            }
            steps.set(stepIndex, updated);
        }
        if (!changed && !analysis.activeSteps().isEmpty()) {
            final int fallbackIndex = analysis.activeSteps().get(analysis.activeSteps().size() - 1);
            final MelodicPattern.Step step = steps.get(fallbackIndex);
            steps.set(fallbackIndex, step.withPitch(context.pitchForDegree(0, 1)));
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern varyEnding(final MelodicPattern pattern, final MelodicPhraseContext context,
                                      final double intensity, final long seed) {
        final Random random = new Random(seed);
        final int start = Math.max(0, pattern.loopSteps() - 4);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        final List<Integer> candidates = new ArrayList<>();
        for (int i = start; i < pattern.loopSteps(); i++) {
            if (steps.get(i).active()) {
                candidates.add(i);
            }
        }
        boolean changed = false;
        if (!candidates.isEmpty()) {
            final int phraseLength = Math.min(Math.max(2, changeBudget(intensity) + 1), candidates.size());
            final List<Integer> endingIndices = candidates.subList(candidates.size() - phraseLength, candidates.size());
            int previousDegree = 1 + random.nextInt(3 + (int) Math.round(intensity * 3));
            for (int idx = 0; idx < endingIndices.size(); idx++) {
                final int stepIndex = endingIndices.get(idx);
                final MelodicPattern.Step step = steps.get(stepIndex);
                final boolean finalStep = idx == endingIndices.size() - 1;
                final int motion = finalStep ? (random.nextBoolean() ? -1 : 1) : random.nextInt(3) - 1;
                previousDegree = Math.max(0, Math.min(7, previousDegree + motion));
                final int octaveOffset = finalStep && random.nextDouble() < 0.35 + intensity * 0.2 ? -1 : 0;
                final MelodicPattern.Step updated = step
                        .withPitch(context.pitchForDegree(octaveOffset, previousDegree))
                        .withAccent(finalStep || step.accent() || random.nextDouble() < 0.25)
                        .withGate(Math.max(step.gate(), finalStep ? 0.92 : 0.82));
                if (!updated.equals(step)) {
                    changed = true;
                }
                steps.set(stepIndex, updated);
            }
        }
        if (!changed && pattern.loopSteps() > 0) {
            final int lastIndex = pattern.loopSteps() - 1;
            final MelodicPattern.Step step = steps.get(lastIndex);
            if (step.active()) {
                steps.set(lastIndex, step.withPitch(context.pitchForDegree(-1, 2)).withAccent(true).withGate(0.95));
            }
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern simplify(final MelodicPattern pattern, final double intensity,
                                    final double identityPreserve, final long seed) {
        final Random random = new Random(seed);
        final MelodicPatternAnalyzer.Analysis analysis = MelodicPatternAnalyzer.analyze(pattern);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        final List<Integer> candidates = new ArrayList<>();
        for (final int stepIndex : analysis.activeSteps()) {
            if (!analysis.anchorSteps().contains(stepIndex) || random.nextDouble() >= identityPreserve) {
                candidates.add(stepIndex);
            }
        }
        Collections.shuffle(candidates, random);
        boolean removed = false;
        for (final int stepIndex : candidates.subList(0, Math.min(changeBudget(intensity), candidates.size()))) {
            steps.set(stepIndex, MelodicPattern.Step.rest(stepIndex));
            removed = true;
        }
        if (!removed && analysis.activeSteps().size() > 1) {
            final int fallbackIndex = analysis.activeSteps().get(analysis.activeSteps().size() - 1);
            steps.set(fallbackIndex, MelodicPattern.Step.rest(fallbackIndex));
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern densify(final MelodicPattern pattern, final MelodicPhraseContext context,
                                   final double intensity, final long seed) {
        final Random random = new Random(seed);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        final List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (!steps.get(i).active()) {
                candidates.add(i);
            }
        }
        Collections.shuffle(candidates, random);
        boolean added = false;
        for (final int i : candidates.subList(0, Math.min(changeBudget(intensity), candidates.size()))) {
            final int degree = random.nextInt(4);
            steps.set(i, new MelodicPattern.Step(i, true, false, context.pitchForDegree(0, degree),
                    84 + random.nextInt(18), 0.68, false, false));
            added = true;
        }
        if (!added) {
            for (int i = 0; i < pattern.loopSteps(); i++) {
                final MelodicPattern.Step step = steps.get(i);
                if (!step.active()) {
                    steps.set(i, new MelodicPattern.Step(i, true, false, context.pitchForDegree(0, 1),
                            92, 0.68, false, false));
                    break;
                }
            }
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private int changeBudget(final double intensity) {
        return intensity >= 0.75 ? 2 : 1;
    }
}
