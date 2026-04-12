package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class EuclideanPhraseGenerator implements MelodicGenerator {
    private static final int[] DEGREE_POOL = {0, 2, 4, 1, 3, 5};

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final int pulses = Math.max(1, Math.min(loopSteps, parameters.pulses()));
        final Random random = new Random(parameters.seed());
        final boolean[] rhythm = euclidean(loopSteps, pulses, parameters.rotation());
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        int phraseIndex = 0;
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps || !rhythm[i]) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean anchor = i == 0 || i % 4 == 0;
            final int degreeLimit = Math.max(2, 2 + (int) Math.round(parameters.tension() * 4));
            final int degree = DEGREE_POOL[(phraseIndex + random.nextInt(degreeLimit)) % DEGREE_POOL.length];
            final int octaveOffset = random.nextDouble() < parameters.octaveActivity() * 0.25
                    ? (phraseIndex % 2 == 0 ? 1 : -1)
                    : 0;
            final int pitch = context.pitchForDegree(octaveOffset, degree);
            final boolean accent = anchor || phraseIndex % 3 == 0;
            final boolean slide = !anchor && random.nextDouble() < parameters.tension() * 0.1;
            final double gate = slide ? 0.96 + parameters.legato() * 0.18 : 0.7 + parameters.density() * 0.2;
            final int velocity = accent ? 116 : 82 + random.nextInt(20);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
            phraseIndex++;
        }
        return new MelodicPattern(steps, loopSteps);
    }

    static boolean[] euclidean(final int steps, final int pulses, final int rotation) {
        final boolean[] pattern = new boolean[steps];
        for (int i = 0; i < steps; i++) {
            final int lhs = Math.floorMod(i * pulses, steps);
            final int rhs = Math.floorMod((i - 1) * pulses, steps);
            pattern[i] = lhs < pulses && (i == 0 || lhs < rhs);
        }
        final boolean[] rotated = new boolean[steps];
        for (int i = 0; i < steps; i++) {
            rotated[Math.floorMod(i + rotation, steps)] = pattern[i];
        }
        return rotated;
    }
}
