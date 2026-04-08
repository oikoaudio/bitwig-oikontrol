package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AcidGenerator implements MelodicGenerator {
    private static final int[] CORE_DEGREES = {0, 1, 2, 3, 4, 5, 6};
    private static final int[] FILL_DEGREES = {0, 2, 3, 4, 5, 6, 7};
    private static final boolean[] ANCHOR_STEPS = {
            true, false, false, false,
            true, false, false, false,
            true, false, false, false,
            true, false, true, false
    };

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        int previousDegree = 0;
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean anchor = i < ANCHOR_STEPS.length && ANCHOR_STEPS[i];
            final boolean active = anchor
                    || random.nextDouble() < 0.12 + parameters.density() * 0.33 + (i % 4 == 2 ? 0.08 : 0.0);
            if (!active) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean accent = anchor || random.nextDouble() < 0.14 + parameters.tension() * 0.28;
            final boolean slide = i < loopSteps - 1 && random.nextDouble() < 0.12 + parameters.tension() * 0.28;
            final int degree = pickDegree(random, previousDegree, anchor, parameters.tension());
            previousDegree = degree;
            final int octaveOffset = octaveOffset(random, parameters.octaveActivity(), anchor);
            final int pitch = context.pitchForDegree(octaveOffset, degree);
            final double gate = slide ? 1.12 : (accent ? 0.95 : 0.82 + parameters.density() * 0.12);
            final int velocity = accent ? 120 : 88 + random.nextInt(16);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }
        MelodicPattern pattern = new MelodicPattern(steps, loopSteps);
        for (int i = 1; i < loopSteps; i++) {
            final MelodicPattern.Step previous = pattern.step(i - 1);
            final MelodicPattern.Step current = pattern.step(i);
            if (!previous.active() || current.active()) {
                continue;
            }
            if (random.nextDouble() < 0.08 + parameters.tension() * 0.18) {
                pattern = pattern.withStep(current.withTieFromPrevious(true));
            }
        }
        if (activeCount(pattern) < Math.max(4, loopSteps / 4)) {
            pattern = addFallbackActivity(pattern, context, random);
        }
        return pattern;
    }

    private MelodicPattern addFallbackActivity(final MelodicPattern pattern, final MelodicPhraseContext context,
                                               final Random random) {
        MelodicPattern out = pattern;
        final int[] fallbackSteps = {2, 6, 10, 14};
        for (final int stepIndex : fallbackSteps) {
            if (stepIndex >= out.loopSteps()) {
                continue;
            }
            final MelodicPattern.Step step = out.step(stepIndex);
            if (step.active()) {
                continue;
            }
            final int degree = FILL_DEGREES[random.nextInt(FILL_DEGREES.length)];
            out = out.withStep(new MelodicPattern.Step(stepIndex, true, false, context.pitchForDegree(0, degree),
                    92 + random.nextInt(12), 0.84, false, false));
        }
        return out;
    }

    private int pickDegree(final Random random, final int previousDegree, final boolean anchor, final double tension) {
        if (anchor) {
            return random.nextDouble() < 0.65 ? 0 : CORE_DEGREES[random.nextInt(Math.min(4, CORE_DEGREES.length))];
        }
        final int[] palette = random.nextDouble() < 0.55 + tension * 0.25 ? CORE_DEGREES : FILL_DEGREES;
        final int maxLeap = tension >= 0.6 ? 3 : 2;
        final List<Integer> candidates = new ArrayList<>();
        for (final int degree : palette) {
            if (Math.abs(degree - previousDegree) <= maxLeap || degree == 0) {
                candidates.add(degree);
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(previousDegree);
            candidates.add(0);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private int octaveOffset(final Random random, final double octaveActivity, final boolean anchor) {
        final double chance = anchor ? octaveActivity * 0.18 : octaveActivity * 0.35;
        if (random.nextDouble() >= chance) {
            return 0;
        }
        if (random.nextDouble() < octaveActivity * 0.15) {
            return random.nextBoolean() ? 2 : -1;
        }
        return random.nextBoolean() ? 1 : -1;
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
}
