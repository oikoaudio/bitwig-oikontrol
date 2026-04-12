package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class MelodicRecurrencePlanner {
    private MelodicRecurrencePlanner() {
    }

    public static MelodicPattern apply(final MelodicPattern pattern, final MelodicPhraseContext context,
                                       final double timeVariance, final long seed) {
        if (timeVariance < 0.12) {
            return clearRecurrence(pattern);
        }

        final Random random = new Random(seed ^ 0x5EEDC0DEL);
        MelodicPattern out = clearRecurrence(pattern);
        final MelodicPatternAnalyzer.Analysis analysis = MelodicPatternAnalyzer.analyze(out);
        final List<Integer> candidates = new ArrayList<>(analysis.activeSteps());
        candidates.removeAll(analysis.anchorSteps());
        candidates.sort(Comparator.comparingInt(MelodicRecurrencePlanner::priorityForStep));

        final int span = timeVariance < 0.34 ? 2 : timeVariance < 0.67 ? 4 : 8;
        final int assignmentTarget = Math.max(1,
                Math.min(candidates.size(), (int) Math.round(candidates.size() * (0.15 + timeVariance * 0.55))));

        for (int i = 0; i < assignmentTarget && i < candidates.size(); i++) {
            final int stepIndex = candidates.get(i);
            final MelodicPattern.Step step = out.step(stepIndex);
            final int mask = chooseMask(span, step, timeVariance, random);
            out = out.withStep(step.withRecurrence(span, mask));
        }

        if (timeVariance >= 0.6) {
            out = maybeInjectOrnament(out, context, span, random, timeVariance);
        }
        return out;
    }

    private static MelodicPattern clearRecurrence(final MelodicPattern pattern) {
        MelodicPattern out = pattern;
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = out.step(i);
            if (step.recurrenceLength() > 0 || step.recurrenceMask() != 0) {
                out = out.withStep(step.withRecurrence(0, 0));
            }
        }
        return out;
    }

    private static int priorityForStep(final int stepIndex) {
        final int beatBias = stepIndex % 4 == 0 ? 100 : stepIndex % 2 == 0 ? 50 : 0;
        return beatBias + stepIndex;
    }

    private static int chooseMask(final int span, final MelodicPattern.Step step, final double timeVariance,
                                  final Random random) {
        final int[] masks = switch (span) {
            case 2 -> new int[]{0b01};
            case 4 -> timeVariance < 0.55
                    ? new int[]{0b0011, 0b0101, 0b0111}
                    : new int[]{0b0011, 0b0101, 0b0111, 0b1001, 0b1011};
            default -> step.accent()
                    ? new int[]{0b00001111, 0b01010101, 0b01110111}
                    : new int[]{0b00001111, 0b00110011, 0b01010101, 0b01110111, 0b10011001};
        };
        return masks[random.nextInt(masks.length)];
    }

    private static MelodicPattern maybeInjectOrnament(final MelodicPattern pattern, final MelodicPhraseContext context,
                                                      final int span, final Random random, final double timeVariance) {
        final List<Integer> ornamentSlots = new ArrayList<>();
        for (int i = 1; i < pattern.loopSteps() - 1; i++) {
            if (pattern.step(i).active()) {
                continue;
            }
            final MelodicPattern.Step previous = pattern.step(i - 1);
            final MelodicPattern.Step next = pattern.step(i + 1);
            if (!previous.active() || previous.pitch() == null || !next.active() || next.pitch() == null) {
                continue;
            }
            if (i % 4 == 0) {
                continue;
            }
            ornamentSlots.add(i);
        }
        if (ornamentSlots.isEmpty()) {
            return pattern;
        }
        final int slot = ornamentSlots.get(random.nextInt(ornamentSlots.size()));
        final MelodicPattern.Step previous = pattern.step(slot - 1);
        final MelodicPattern.Step next = pattern.step(slot + 1);
        final int lower = Math.min(previous.pitch(), next.pitch());
        final int upper = Math.max(previous.pitch(), next.pitch());
        final int pitch = lower == upper
                ? context.pitchForDegree(0, 1)
                : lower + Math.max(1, (upper - lower) / 2);
        final int mask = span >= 8 && timeVariance > 0.8 ? 0b10101000 : 0b00001001;
        return pattern.withStep(new MelodicPattern.Step(slot, true, false, pitch, 92, 0.55, false, false)
                .withRecurrence(span, mask));
    }
}
