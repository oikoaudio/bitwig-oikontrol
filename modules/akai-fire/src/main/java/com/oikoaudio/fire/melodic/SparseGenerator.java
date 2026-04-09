package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SparseGenerator implements MelodicGenerator {
    private static final int[] SPARSE_POSITIONS = {0, 4, 8, 12, 15};
    private static final int[] SPARSE_DEGREES = {0, 2, 4};

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        for (final int position : SPARSE_POSITIONS) {
            if (position >= loopSteps) {
                continue;
            }
            if (position != 0 && position != loopSteps - 1 && random.nextDouble() > 0.7 + parameters.density() * 0.2) {
                continue;
            }
            final int degree = SPARSE_DEGREES[random.nextInt(Math.max(1, 1 + (int) Math.round(parameters.tension() * 2)))];
            final int octaveOffset = random.nextDouble() < parameters.octaveActivity() * 0.2 ? 1 : 0;
            final int pitch = context.pitchForDegree(octaveOffset, degree);
            final boolean accent = position == 0 || position == loopSteps - 1 || position % 8 == 0;
            steps.set(position, new MelodicPattern.Step(position, true, false, pitch, accent ? 116 : 94,
                    accent ? 0.88 : 0.76, accent, false));
        }
        return new MelodicPattern(steps, loopSteps);
    }
}
