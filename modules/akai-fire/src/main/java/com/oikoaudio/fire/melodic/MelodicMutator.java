package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.sequence.RecurrencePattern;

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
        final MelodicPattern base = stripAlternateVariants(pattern);
        final TimeVariationProfile profile = timeVariationProfile(recurrenceStyle, intensity);
        MelodicPattern varied = base;
        for (int attempt = 0; attempt < 4; attempt++) {
            final long attemptSeed = seed + attempt * 97L;
            MelodicPattern planned = MelodicRecurrencePlanner.apply(base, context, recurrenceStyle, timeVariance, attemptSeed);
            planned = ensureRecurringDensity(base, planned, recurrenceStyle, timeVariance, intensity, attemptSeed);
            varied = addAlternateRecurrenceVoices(planned, context, recurrenceStyle, profile, attemptSeed);
            varied = ensurePhraseAlternatePresence(varied, context, recurrenceStyle, attemptSeed);
            if (!varied.equals(pattern)) {
                return varied;
            }
        }
        return forceDifferentTimeVariation(varied, context, recurrenceStyle, timeVariance, seed);
    }

    private MelodicPattern stripAlternateVariants(final MelodicPattern pattern) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps().size());
        for (final MelodicPattern.Step step : pattern.steps()) {
            steps.add(step.hasAlternate() ? step.withoutAlternate() : step);
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern addAlternateRecurrenceVoices(final MelodicPattern pattern, final MelodicPhraseContext context,
                                                        final MelodicRecurrencePlanner.Style style,
                                                        final TimeVariationProfile profile, final long seed) {
        final Random random = new Random(seed ^ 0x41A7E11AL);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        final boolean[] claimed = new boolean[pattern.loopSteps()];
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = steps.get(i);
            if (!step.active() || step.pitch() == null || step.recurrenceLength() <= 1 || step.hasAlternate() || claimed[i]) {
                continue;
            }
            final List<Integer> phraseIndices = collectAlternatePhraseIndices(style, profile, pattern, i, claimed, random);
            if (phraseIndices.size() >= 2
                    && random.nextDouble() < phraseRateFor(style, profile, pattern, step, i)) {
                applyAlternatePhraseRun(steps, context, random, claimed, phraseIndices);
                continue;
            }
            if (shouldAddSingleAlternate(style, profile, pattern, step, i, random)) {
                applyAlternateAtStep(steps, context, random, i, null, nonZeroMotion(random, 1, 2 + profile.maxMotion()));
                claimed[i] = true;
            }
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern ensureRecurringDensity(final MelodicPattern sourcePattern, final MelodicPattern planned,
                                                  final MelodicRecurrencePlanner.Style style,
                                                  final double timeVariance, final double intensity,
                                                  final long seed) {
        final int minimumRecurringSteps = minimumRecurringSteps(style, sourcePattern.loopSteps(), intensity, timeVariance);
        if (minimumRecurringSteps <= 0 || recurringCarrierCount(planned) >= minimumRecurringSteps) {
            return planned;
        }
        final List<Integer> candidates = rankedRecurringAugmentCandidates(sourcePattern, planned, style);
        if (candidates.isEmpty()) {
            return planned;
        }
        final Random random = new Random(seed ^ 0x5A17B3D4L);
        final int span = recurrenceSpanFor(timeVariance);
        MelodicPattern out = planned;
        int ordinal = 0;
        for (final int stepIndex : candidates) {
            if (recurringCarrierCount(out) >= minimumRecurringSteps) {
                break;
            }
            final MelodicPattern.Step step = out.step(stepIndex);
            if (!step.active() || step.pitch() == null || step.recurrenceLength() > 1) {
                continue;
            }
            out = out.withStep(step.withRecurrence(span, supplementalMask(style, span, ordinal++, random)));
        }
        return out;
    }

    private int minimumRecurringSteps(final MelodicRecurrencePlanner.Style style, final int loopSteps,
                                      final double intensity, final double timeVariance) {
        if (timeVariance < 0.2) {
            return 0;
        }
        return switch (style) {
            case ACID -> timeVariance >= 0.9
                    ? (loopSteps >= 24 ? 4 : 3)
                    : timeVariance >= 0.55 ? 3 : 2;
            case CALL_RESPONSE, MOTIF -> timeVariance >= 0.8 ? 3 : 2;
            case ROLLING, OCTAVE -> intensity >= 0.75 ? 2 : 1;
        };
    }

    private int recurrenceSpanFor(final double timeVariance) {
        if (timeVariance < 0.34) {
            return 2;
        }
        if (timeVariance < 0.67) {
            return 4;
        }
        return 8;
    }

    private List<Integer> rankedRecurringAugmentCandidates(final MelodicPattern sourcePattern, final MelodicPattern planned,
                                                           final MelodicRecurrencePlanner.Style style) {
        final List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < sourcePattern.loopSteps(); i++) {
            final MelodicPattern.Step sourceStep = sourcePattern.step(i);
            final MelodicPattern.Step plannedStep = planned.step(i);
            if (!sourceStep.active() || sourceStep.pitch() == null || sourceStep.tieFromPrevious()
                    || plannedStep.recurrenceLength() > 1) {
                continue;
            }
            candidates.add(i);
        }
        candidates.sort((left, right) -> Double.compare(
                candidateAugmentScore(sourcePattern, style, right),
                candidateAugmentScore(sourcePattern, style, left)));
        return candidates;
    }

    private double candidateAugmentScore(final MelodicPattern pattern, final MelodicRecurrencePlanner.Style style,
                                         final int stepIndex) {
        final MelodicPattern.Step step = pattern.step(stepIndex);
        double score = step.accent() ? 1.6 : 1.0;
        if (stepIndex % 4 == 0) {
            score += 1.0;
        } else if (stepIndex % 2 == 0) {
            score += 0.4;
        }
        if (style == MelodicRecurrencePlanner.Style.ACID) {
            if (isAcidHookWindow(stepIndex, pattern.loopSteps())) {
                score += 2.4;
            } else if (isAcidLateWindow(stepIndex, pattern.loopSteps())) {
                score += 1.8;
            } else {
                score += 0.6;
            }
        } else if (style == MelodicRecurrencePlanner.Style.CALL_RESPONSE && stepIndex >= pattern.loopSteps() / 2) {
            score += 1.5;
        }
        return score;
    }

    private int supplementalMask(final MelodicRecurrencePlanner.Style style, final int span, final int ordinal,
                                 final Random random) {
        final int[] masks = switch (span) {
            case 2 -> new int[]{0b01};
            case 4 -> switch (style) {
                case ACID -> new int[]{0b0011, 0b0101, 0b0111};
                case CALL_RESPONSE -> new int[]{0b0011, 0b0101, 0b0111};
                case MOTIF -> new int[]{0b0011, 0b0101};
                case ROLLING, OCTAVE -> new int[]{0b0011, 0b0111};
            };
            default -> switch (style) {
                case ACID -> new int[]{0b00000011, 0b00001101, 0b00110011, 0b01010101, 0b01110001};
                case CALL_RESPONSE -> new int[]{0b00010001, 0b00110011, 0b01000101, 0b01110001};
                case MOTIF -> new int[]{0b00010001, 0b00100011, 0b01000101, 0b00110011};
                case ROLLING -> new int[]{0b00010001, 0b00100010, 0b01000100};
                case OCTAVE -> new int[]{0b00010001, 0b00100100, 0b01000100};
            };
        };
        return masks[(ordinal + random.nextInt(masks.length)) % masks.length];
    }

    private MelodicPattern ensurePhraseAlternatePresence(final MelodicPattern pattern, final MelodicPhraseContext context,
                                                         final MelodicRecurrencePlanner.Style style, final long seed) {
        if (style != MelodicRecurrencePlanner.Style.ACID || hasAlternateVoices(pattern)) {
            return pattern;
        }
        final List<Integer> recurrent = recurrentCandidateIndices(pattern);
        if (recurrent.size() < 2) {
            return pattern;
        }
        final List<Integer> phraseIndices = forcedPhraseIndices(recurrent);
        if (phraseIndices.size() < 2) {
            return pattern;
        }
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        applyAlternatePhraseRun(steps, context, new Random(seed ^ 0x2E8E7C5DL),
                new boolean[pattern.loopSteps()], phraseIndices);
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private boolean hasAlternateVoices(final MelodicPattern pattern) {
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (pattern.step(i).hasAlternate()) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> recurrentCandidateIndices(final MelodicPattern pattern) {
        final List<Integer> recurrent = new ArrayList<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.active() && step.pitch() != null && step.recurrenceLength() > 1 && !step.tieFromPrevious()) {
                recurrent.add(i);
            }
        }
        return recurrent;
    }

    private List<Integer> forcedPhraseIndices(final List<Integer> recurrent) {
        final List<Integer> phrase = new ArrayList<>();
        phrase.add(recurrent.get(0));
        for (int i = 1; i < recurrent.size() && phrase.size() < 3; i++) {
            final int candidate = recurrent.get(i);
            final int previous = phrase.get(phrase.size() - 1);
            if (candidate - previous <= 4 || phrase.size() < 2) {
                phrase.add(candidate);
            }
        }
        return phrase;
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

    private boolean isAcidHookWindow(final int stepIndex, final int loopSteps) {
        final int start = loopSteps / 4;
        final int end = loopSteps / 2;
        return stepIndex >= start && stepIndex < end;
    }

    private boolean isAcidLateWindow(final int stepIndex, final int loopSteps) {
        return stepIndex >= Math.max(loopSteps / 2, loopSteps - Math.max(4, loopSteps / 4));
    }

    private boolean shouldAddSingleAlternate(final MelodicRecurrencePlanner.Style style,
                                             final TimeVariationProfile profile,
                                             final MelodicPattern pattern,
                                             final MelodicPattern.Step step,
                                             final int stepIndex,
                                             final Random random) {
        final double roleBias = step.accent() ? -0.08 : 0.10;
        double rate = profile.singleAlternateRate() + roleBias;
        if (style == MelodicRecurrencePlanner.Style.ACID) {
            rate -= 0.22;
        }
        if (style == MelodicRecurrencePlanner.Style.CALL_RESPONSE && stepIndex >= pattern.loopSteps() / 2) {
            rate += 0.08;
        }
        if (style == MelodicRecurrencePlanner.Style.ROLLING) {
            rate -= 0.06;
        }
        return random.nextDouble() < Math.max(0.0, Math.min(0.8, rate));
    }

    private List<Integer> collectAlternatePhraseIndices(final MelodicRecurrencePlanner.Style style,
                                                        final TimeVariationProfile profile,
                                                        final MelodicPattern pattern,
                                                        final int startIndex,
                                                        final boolean[] claimed,
                                                        final Random random) {
        final List<Integer> indices = new ArrayList<>();
        indices.add(startIndex);
        final int target = profile.preferThreeStepPhrases() && random.nextDouble() < 0.55 ? 3 : 2;
        final int maxGap = maxPhraseGap(style);
        int previousIndex = startIndex;
        for (int i = startIndex + 1; i < pattern.loopSteps() && indices.size() < target; i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (claimed[i] || !step.active() || step.pitch() == null || step.recurrenceLength() <= 1 || step.tieFromPrevious()) {
                continue;
            }
            if (style == MelodicRecurrencePlanner.Style.CALL_RESPONSE && i < pattern.loopSteps() / 2) {
                continue;
            }
            if (i - previousIndex > maxGap) {
                break;
            }
            indices.add(i);
            previousIndex = i;
        }
        return indices;
    }

    private int maxPhraseGap(final MelodicRecurrencePlanner.Style style) {
        return switch (style) {
            case ACID -> 3;
            case MOTIF, CALL_RESPONSE -> 2;
            case ROLLING, OCTAVE -> 1;
        };
    }

    private void applyAlternatePhraseRun(final List<MelodicPattern.Step> steps, final MelodicPhraseContext context,
                                         final Random random, final boolean[] claimed,
                                         final List<Integer> phraseIndices) {
        final int baseMotion = nonZeroMotion(random, 1, 3);
        Integer previousTargetPitch = null;
        for (int offset = 0; offset < phraseIndices.size(); offset++) {
            final int stepIndex = phraseIndices.get(offset);
            final int motion = offset == 0 ? baseMotion : adjustRunMotion(baseMotion, offset, random);
            previousTargetPitch = applyAlternateAtStep(steps, context, random, stepIndex, previousTargetPitch, motion);
            claimed[stepIndex] = true;
        }
    }

    private int adjustRunMotion(final int baseMotion, final int offset, final Random random) {
        final int direction = baseMotion < 0 ? -1 : 1;
        final int magnitude = Math.max(1, Math.abs(baseMotion) + (random.nextDouble() < 0.35 ? offset : 0));
        return direction * magnitude;
    }

    private Integer applyAlternateAtStep(final List<MelodicPattern.Step> steps, final MelodicPhraseContext context,
                                         final Random random, final int stepIndex,
                                         final Integer previousTargetPitch, final int motion) {
        final MelodicPattern.Step step = steps.get(stepIndex);
        final RecurrencePattern recurrence = RecurrencePattern.of(step.recurrenceLength(), step.recurrenceMask());
        final int span = recurrence.effectiveSpan();
        final int[] splitMasks = splitRecurrenceMasks(recurrence.effectiveMask(span), span, random);
        final int mainMask = splitMasks[0];
        final int alternateMask = splitMasks[1];
        if (alternateMask == 0 || alternateMask == ((1 << span) - 1)) {
            return previousTargetPitch;
        }
        final int targetPitch = previousTargetPitch != null
                ? chooseDifferentNearbyPitch(context, previousTargetPitch, motion, step.pitch())
                : chooseDifferentNearbyPitch(context, step.pitch(), motion, step.pitch());
        final MelodicPattern.Step updated = step
                .withRecurrence(span, mainMask)
                .withAlternate(targetPitch,
                        Math.max(1, step.velocity() - (step.accent() ? 10 : 4)),
                        Math.max(0.35, step.gate() - 0.06),
                        step.chance(),
                        false,
                        false,
                        span,
                        alternateMask);
        steps.set(stepIndex, updated);
        return targetPitch;
    }

    private double phraseRateFor(final MelodicRecurrencePlanner.Style style,
                                 final TimeVariationProfile profile,
                                 final MelodicPattern pattern,
                                 final MelodicPattern.Step step,
                                 final int stepIndex) {
        double rate = profile.phraseAlternateRate() + (step.accent() ? 0.05 : 0.0);
        if (style == MelodicRecurrencePlanner.Style.CALL_RESPONSE && stepIndex >= pattern.loopSteps() / 2) {
            rate += 0.12;
        }
        if (style == MelodicRecurrencePlanner.Style.ACID && stepIndex >= pattern.loopSteps() / 4) {
            rate += 0.06;
        }
        return Math.max(0.0, Math.min(0.85, rate));
    }

    private TimeVariationProfile timeVariationProfile(final MelodicRecurrencePlanner.Style style, final double intensity) {
        return switch (style) {
            case ACID -> new TimeVariationProfile(0.0, 0.62 + intensity * 0.24, true,
                    2 + (int) Math.round(intensity * 2));
            case MOTIF -> new TimeVariationProfile(0.18 + intensity * 0.18, 0.38 + intensity * 0.24, true,
                    2 + (int) Math.round(intensity * 2));
            case CALL_RESPONSE -> new TimeVariationProfile(0.16 + intensity * 0.14, 0.42 + intensity * 0.24, true,
                    2 + (int) Math.round(intensity * 2));
            case ROLLING -> new TimeVariationProfile(0.10 + intensity * 0.10, 0.12 + intensity * 0.10, false,
                    1 + (int) Math.round(intensity));
            case OCTAVE -> new TimeVariationProfile(0.20 + intensity * 0.16, 0.18 + intensity * 0.12, false,
                    2 + (int) Math.round(intensity));
        };
    }

    private record TimeVariationProfile(double singleAlternateRate,
                                        double phraseAlternateRate,
                                        boolean preferThreeStepPhrases,
                                        int maxMotion) {
    }

    private int[] splitRecurrenceMasks(final int sourceMask, final int span, final Random random) {
        final int fullMask = (1 << span) - 1;
        int mainMask = sourceMask & fullMask;
        if (mainMask == 0) {
            mainMask = 0b1;
        }

        final List<Integer> movableBits = new ArrayList<>();
        for (int bit = 1; bit < span; bit++) {
            if (((mainMask >> bit) & 0x1) == 1) {
                movableBits.add(bit);
            }
        }
        Collections.shuffle(movableBits, random);

        int alternateMask = 0;
        final int transferCount = Math.max(1, movableBits.size() / 2);
        for (int i = 0; i < Math.min(transferCount, movableBits.size()); i++) {
            final int bit = movableBits.get(i);
            mainMask &= ~(1 << bit);
            alternateMask |= 1 << bit;
        }

        if (alternateMask == 0) {
            for (int bit = 1; bit < span; bit++) {
                if (((mainMask >> bit) & 0x1) == 0) {
                    alternateMask |= 1 << bit;
                    break;
                }
            }
        }
        if (alternateMask == 0) {
            return new int[]{sourceMask & fullMask, 0};
        }
        return new int[]{mainMask, alternateMask};
    }

    private MelodicPattern forceDifferentTimeVariation(final MelodicPattern pattern, final MelodicPhraseContext context,
                                                       final MelodicRecurrencePlanner.Style recurrenceStyle,
                                                       final double timeVariance, final long seed) {
        final Random random = new Random(seed ^ 0x7711E5AAL);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        final List<Integer> recurrent = new ArrayList<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (pattern.step(i).recurrenceLength() > 1) {
                recurrent.add(i);
            }
        }
        if (!recurrent.isEmpty()) {
            final int stepIndex = recurrent.get(random.nextInt(recurrent.size()));
            final MelodicPattern.Step step = steps.get(stepIndex);
            final int span = RecurrencePattern.of(step.recurrenceLength(), step.recurrenceMask()).effectiveSpan();
            final int rotatedMask = rotateMask(step.recurrenceMask(), span, 1 + random.nextInt(Math.max(1, span - 1)));
            steps.set(stepIndex, step.withoutAlternate().withRecurrence(span, rotatedMask));
            return new MelodicPattern(steps, pattern.loopSteps());
        }

        final MelodicPattern replanned = MelodicRecurrencePlanner.apply(pattern, context, recurrenceStyle,
                Math.min(1.0, timeVariance + 0.1), seed ^ 0x13579BDFL);
        if (!replanned.equals(pattern)) {
            return replanned;
        }
        return pattern;
    }

    private int rotateMask(final int mask, final int span, final int amount) {
        final int normalizedAmount = Math.floorMod(amount, span);
        final int fullMask = (1 << span) - 1;
        final int normalizedMask = mask & fullMask;
        return ((normalizedMask << normalizedAmount) | (normalizedMask >>> (span - normalizedAmount))) & fullMask;
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
