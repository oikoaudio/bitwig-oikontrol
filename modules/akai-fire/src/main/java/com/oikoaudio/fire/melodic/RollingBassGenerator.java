package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RollingBassGenerator implements MelodicGenerator {
    private static final int[] CORE_DEGREES = {0, 1, 2, 3, 4, 5, 6};
    private static final int[] FILL_DEGREES = {0, 2, 4, 5, 7, 9};

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
            final boolean forceActive = i % 4 != 3 || random.nextDouble() < 0.85;
            if (!forceActive && random.nextDouble() > parameters.density() * 0.25) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final int degree = chooseDegree(random, previousDegree, i, parameters);
            previousDegree = degree;
            final int octaveOffset = chooseOctaveOffset(random, parameters.octaveActivity(), i);
            final int pitch = context.pitchForDegree(octaveOffset, degree);
            final boolean accent = i % 4 == 0 || i == loopSteps - 1;
            final double gate = 0.98 + parameters.density() * 0.12;
            final int velocity = accent ? 114 : 84 + (i % 2 == 0 ? 10 : 0);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, false));
        }
        return new MelodicPattern(steps, loopSteps);
    }

    private int chooseDegree(final Random random, final int previousDegree, final int stepIndex,
                             final GenerateParameters parameters) {
        if (stepIndex % 4 == 0) {
            return random.nextDouble() < 0.45 ? 0 : CORE_DEGREES[random.nextInt(Math.min(5, CORE_DEGREES.length))];
        }
        final int maxLeap = parameters.tension() >= 0.45 ? 3 : 2;
        final List<Integer> candidates = new ArrayList<>();
        for (final int degree : CORE_DEGREES) {
            if (Math.abs(degree - previousDegree) <= maxLeap) {
                candidates.add(degree);
            }
        }
        if (parameters.tension() >= 0.3 || random.nextDouble() < 0.25) {
            for (final int degree : FILL_DEGREES) {
                if (Math.abs(degree - previousDegree) <= maxLeap + 1) {
                    candidates.add(degree);
                }
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(previousDegree);
            candidates.add(0);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private int chooseOctaveOffset(final Random random, final double octaveActivity, final int stepIndex) {
        final double chance = stepIndex % 4 == 0 ? octaveActivity * 0.15 : octaveActivity * 0.25;
        if (random.nextDouble() >= chance) {
            return 0;
        }
        return random.nextDouble() < 0.75 ? 1 : -1;
    }
}
