package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class MelodicMutator {
    private static final int SCALE_SEARCH_COUNT = 48;

    public enum Mode {
        PRESERVE_RHYTHM,
        VARY_ENDING,
        SIMPLIFY,
        DENSIFY,
        VARY_TIME
    }

    public MelodicPattern mutate(final MelodicPattern pattern, final MelodicPhraseContext context,
                                 final MelodicRecurrencePlanner.Style recurrenceStyle,
                                 final Mode mode, final double intensity, final double identityPreserve,
                                 final long seed) {
        return switch (mode) {
            case PRESERVE_RHYTHM -> preserveRhythm(pattern, context, intensity, identityPreserve, seed);
            case VARY_ENDING -> varyEnding(pattern, context, intensity, seed);
            case SIMPLIFY -> simplify(pattern, intensity, identityPreserve, seed);
            case DENSIFY -> densify(pattern, context, intensity, seed);
            case VARY_TIME -> varyTime(pattern, context, recurrenceStyle, intensity, seed);
        };
    }

    private MelodicPattern varyTime(final MelodicPattern pattern, final MelodicPhraseContext context,
                                    final MelodicRecurrencePlanner.Style recurrenceStyle,
                                    final double intensity, final long seed) {
        final double timeVariance = Math.max(0.18, Math.min(1.0, 0.2 + intensity * 0.8));
        return MelodicRecurrencePlanner.apply(pattern, context, recurrenceStyle, timeVariance, seed);
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
        Integer previousPitch = null;
        for (final int stepIndex : candidates.subList(0, Math.min(changeBudget(intensity), candidates.size()))) {
            final MelodicPattern.Step step = steps.get(stepIndex);
            final int sourcePitch = step.pitch() != null
                    ? step.pitch()
                    : previousPitch != null ? previousPitch : context.pitchForDegree(0, 0);
            final int motion = nonZeroMotion(random, 1, 1 + (int) Math.round(intensity * 2));
            final int targetPitch = chooseDifferentNearbyPitch(context, sourcePitch, motion, previousPitch);
            final MelodicPattern.Step updated = step.withPitch(targetPitch);
            if (!updated.equals(step)) {
                changed = true;
            }
            steps.set(stepIndex, updated);
            previousPitch = targetPitch;
        }
        if (!changed && !analysis.activeSteps().isEmpty()) {
            final int fallbackIndex = analysis.activeSteps().get(analysis.activeSteps().size() - 1);
            final MelodicPattern.Step step = steps.get(fallbackIndex);
            final int sourcePitch = step.pitch() != null ? step.pitch() : context.pitchForDegree(0, 0);
            steps.set(fallbackIndex, step.withPitch(chooseDifferentNearbyPitch(context, sourcePitch, 1, null)));
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern varyEnding(final MelodicPattern pattern, final MelodicPhraseContext context,
                                      final double intensity, final long seed) {
        final Random random = new Random(seed);
        final int start = Math.max(0, pattern.loopSteps() - 4);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        maybeMutateEndingRhythm(steps, pattern.loopSteps(), start, context, intensity, random);
        final List<Integer> candidates = new ArrayList<>();
        for (int i = start; i < pattern.loopSteps(); i++) {
            if (steps.get(i).active()) {
                candidates.add(i);
            }
        }
        boolean changed = false;
        if (!candidates.isEmpty()) {
            final int maxPhraseLength = Math.min(4, candidates.size());
            final int phraseLength = 1 + random.nextInt(maxPhraseLength);
            final List<Integer> tailCandidates = new ArrayList<>(candidates.subList(candidates.size() - maxPhraseLength, candidates.size()));
            Collections.shuffle(tailCandidates, random);
            final List<Integer> endingIndices = tailCandidates.subList(0, phraseLength).stream()
                    .sorted()
                    .toList();
            Integer previousPitch = null;
            for (int idx = 0; idx < endingIndices.size(); idx++) {
                final int stepIndex = endingIndices.get(idx);
                final MelodicPattern.Step step = steps.get(stepIndex);
                final boolean finalChangedStep = idx == endingIndices.size() - 1;
                final boolean actualLastStep = stepIndex == candidates.get(candidates.size() - 1);
                final int sourcePitch = step.pitch() != null
                        ? step.pitch()
                        : previousPitch != null ? previousPitch : context.pitchForDegree(0, 0);
                final int motion = actualLastStep
                        ? nonZeroMotion(random, 1, 2 + (int) Math.round(intensity * 2))
                        : nonZeroMotion(random, 1, 1 + (int) Math.round(intensity * 2));
                final int octaveBias = actualLastStep && random.nextDouble() < 0.2 + intensity * 0.15 ? -1 : 0;
                int targetPitch = shiftedScalePitch(context, sourcePitch, motion + octaveBias * 7);
                if (previousPitch != null && targetPitch == previousPitch) {
                    final int rescueMotion = motion > 0 ? motion + 1 : motion - 1;
                    targetPitch = shiftedScalePitch(context, sourcePitch, rescueMotion);
                }
                final MelodicPattern.Step updated = step
                        .withPitch(targetPitch)
                        .withAccent(actualLastStep || finalChangedStep || step.accent() || random.nextDouble() < 0.25)
                        .withGate(Math.max(step.gate(), actualLastStep ? 0.92 : 0.82));
                if (!updated.equals(step)) {
                    changed = true;
                }
                steps.set(stepIndex, updated);
                previousPitch = targetPitch;
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

    private void maybeMutateEndingRhythm(final List<MelodicPattern.Step> steps, final int loopSteps, final int start,
                                         final MelodicPhraseContext context, final double intensity, final Random random) {
        if (random.nextDouble() >= 0.45 + intensity * 0.2) {
            return;
        }
        final List<Integer> active = new ArrayList<>();
        final List<Integer> inactive = new ArrayList<>();
        for (int i = start; i < loopSteps; i++) {
            if (steps.get(i).active()) {
                active.add(i);
            } else {
                inactive.add(i);
            }
        }

        final double actionRoll = random.nextDouble();
        if (!inactive.isEmpty() && (active.size() <= 1 || actionRoll < 0.45)) {
            final int stepIndex = inactive.get(random.nextInt(inactive.size()));
            final int sourcePitch = nearestTailPitch(steps, stepIndex, start, loopSteps, context);
            steps.set(stepIndex, new MelodicPattern.Step(stepIndex, true, false, sourcePitch,
                    86 + random.nextInt(18), 0.76, false, false));
            return;
        }

        if (!active.isEmpty() && active.size() > 1 && actionRoll < 0.8) {
            final int removeIndex = active.get(random.nextInt(active.size()));
            steps.set(removeIndex, MelodicPattern.Step.rest(removeIndex));
            if (removeIndex + 1 < loopSteps && steps.get(removeIndex + 1).tieFromPrevious()) {
                steps.set(removeIndex + 1, steps.get(removeIndex + 1).withTieFromPrevious(false));
            }
            return;
        }

        if (!active.isEmpty() && !inactive.isEmpty()) {
            final int fromIndex = active.get(random.nextInt(active.size()));
            final int toIndex = inactive.get(random.nextInt(inactive.size()));
            final MelodicPattern.Step source = steps.get(fromIndex);
            steps.set(fromIndex, MelodicPattern.Step.rest(fromIndex));
            steps.set(toIndex, new MelodicPattern.Step(toIndex, true, false, source.pitch(),
                    source.velocity(), source.gate(), source.chance(), source.accent(), source.slide(),
                    0, 0));
            if (fromIndex + 1 < loopSteps && steps.get(fromIndex + 1).tieFromPrevious()) {
                steps.set(fromIndex + 1, steps.get(fromIndex + 1).withTieFromPrevious(false));
            }
        }
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

    private int nonZeroMotion(final Random random, final int minMagnitude, final int maxMagnitude) {
        final int magnitude = minMagnitude + random.nextInt(Math.max(1, maxMagnitude - minMagnitude + 1));
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    private int shiftedScalePitch(final MelodicPhraseContext context, final int sourcePitch, final int scaleSteps) {
        final List<Integer> scaleNotes = context.collapsedScaleNotes(SCALE_SEARCH_COUNT);
        int nearestIndex = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < scaleNotes.size(); i++) {
            final int distance = Math.abs(scaleNotes.get(i) - sourcePitch);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }
        final int targetIndex = Math.max(0, Math.min(scaleNotes.size() - 1, nearestIndex + scaleSteps));
        return scaleNotes.get(targetIndex);
    }

    private int chooseDifferentNearbyPitch(final MelodicPhraseContext context, final int sourcePitch, final int motion,
                                           final Integer avoidPitch) {
        final List<Integer> scaleNotes = context.collapsedScaleNotes(SCALE_SEARCH_COUNT);
        int nearestIndex = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < scaleNotes.size(); i++) {
            final int distance = Math.abs(scaleNotes.get(i) - sourcePitch);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        final List<Integer> candidates = new ArrayList<>();
        final int preferredOffset = motion == 0 ? 1 : motion;
        for (int extra = -3; extra <= 3; extra++) {
            if (extra == 0) {
                continue;
            }
            final int targetIndex = nearestIndex + preferredOffset + extra;
            if (targetIndex < 0 || targetIndex >= scaleNotes.size()) {
                continue;
            }
            final int candidate = scaleNotes.get(targetIndex);
            if (candidate == sourcePitch || (avoidPitch != null && candidate == avoidPitch)) {
                continue;
            }
            candidates.add(candidate);
        }
        if (!candidates.isEmpty()) {
            candidates.sort((a, b) -> Integer.compare(Math.abs(a - sourcePitch), Math.abs(b - sourcePitch)));
            return candidates.get(0);
        }

        final int fallbackIndex = Math.max(0, Math.min(scaleNotes.size() - 1, nearestIndex + (motion >= 0 ? 1 : -1)));
        return scaleNotes.get(fallbackIndex);
    }

    private int nearestTailPitch(final List<MelodicPattern.Step> steps, final int stepIndex, final int start,
                                 final int loopSteps, final MelodicPhraseContext context) {
        for (int distance = 1; distance < loopSteps - start; distance++) {
            final int left = stepIndex - distance;
            if (left >= start) {
                final MelodicPattern.Step candidate = steps.get(left);
                if (candidate.active() && candidate.pitch() != null) {
                    return candidate.pitch();
                }
            }
            final int right = stepIndex + distance;
            if (right < loopSteps) {
                final MelodicPattern.Step candidate = steps.get(right);
                if (candidate.active() && candidate.pitch() != null) {
                    return candidate.pitch();
                }
            }
        }
        return context.pitchForDegree(0, 0);
    }
}
