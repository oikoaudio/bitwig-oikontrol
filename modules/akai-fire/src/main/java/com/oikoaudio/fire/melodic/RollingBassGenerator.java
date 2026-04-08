package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RollingBassGenerator implements MelodicGenerator {
    private static final int[] ROLLING_DEGREES = {0, 0, 2, 0, 3, 2, 0, 1};

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
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
            final int degree = ROLLING_DEGREES[(i + random.nextInt(2)) % ROLLING_DEGREES.length];
            final int pitch = context.pitchForDegree(0, degree);
            final boolean accent = i % 4 == 0 || i == loopSteps - 1;
            final double gate = 0.98 + parameters.density() * 0.12;
            final int velocity = accent ? 114 : 84 + (i % 2 == 0 ? 10 : 0);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, false));
        }
        return new MelodicPattern(steps, loopSteps);
    }
}
