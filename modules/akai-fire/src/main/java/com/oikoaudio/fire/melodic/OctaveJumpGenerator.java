package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class OctaveJumpGenerator implements MelodicGenerator {
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
            final boolean active = i % 2 == 0 || random.nextDouble() < 0.35 + parameters.density() * 0.25;
            if (!active) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean highOctave = i % 4 == 2 || random.nextDouble() < 0.25 + parameters.octaveActivity() * 0.45;
            final int degree = (i % 8 == 6 && parameters.tension() > 0.4) ? 1 : 0;
            final int pitch = context.pitchForDegree(highOctave ? 1 : 0, degree);
            final boolean accent = i % 4 == 0 || highOctave;
            steps.add(new MelodicPattern.Step(i, true, false, pitch, accent ? 118 : 90,
                    highOctave ? 0.84 : 0.74, accent, false));
        }
        return new MelodicPattern(steps, loopSteps);
    }
}
