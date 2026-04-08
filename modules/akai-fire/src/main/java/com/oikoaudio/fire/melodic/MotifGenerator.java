package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MotifGenerator implements MelodicGenerator {
    private static final int[] CANDIDATE_POSITIONS = {0, 2, 3, 4, 6, 7, 8, 10, 12, 14, 15};
    private static final int[] ANCHOR_DEGREES = {0, 2, 4};
    private static final int[] FILLER_DEGREES = {0, 1, 2, 3, 4, 5, 6};

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        MelodicPattern pattern = MelodicPattern.empty(loopSteps);
        final boolean[] active = new boolean[loopSteps];
        active[0] = true;
        if (loopSteps > 8) {
            active[8] = true;
        }
        active[loopSteps - 1] = true;
        final int targetActiveCount = Math.max(4, Math.min(loopSteps, 4 + (int) Math.round(parameters.density() * 7)));
        while (countActive(active) < targetActiveCount) {
            final int candidate = CANDIDATE_POSITIONS[random.nextInt(CANDIDATE_POSITIONS.length)];
            if (candidate < loopSteps) {
                active[candidate] = true;
            }
        }

        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps || !active[i]) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean anchor = i == 0 || i % 4 == 0 || i == loopSteps - 1;
            final int degree = anchor
                    ? ANCHOR_DEGREES[random.nextInt(ANCHOR_DEGREES.length)]
                    : FILLER_DEGREES[random.nextInt(2 + (int) Math.round(parameters.tension() * 5))];
            final int octaveOffset = random.nextDouble() < parameters.octaveActivity() * 0.35
                    ? (random.nextBoolean() ? 1 : -1)
                    : 0;
            final int pitch = context.pitchForDegree(octaveOffset, degree);
            final boolean accent = anchor || random.nextDouble() < parameters.tension() * 0.2;
            final boolean slide = random.nextDouble() < parameters.tension() * 0.12 && i < loopSteps - 1;
            final double gate = slide ? 1.05 : (accent ? 0.92 : 0.74 + parameters.density() * 0.18);
            final int velocity = accent ? 118 : 86 + random.nextInt(18);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }
        pattern = new MelodicPattern(steps, loopSteps);
        return maybeAddTies(pattern, parameters, random);
    }

    private MelodicPattern maybeAddTies(final MelodicPattern input, final GenerateParameters parameters,
                                        final Random random) {
        MelodicPattern pattern = input;
        for (int i = 0; i < input.loopSteps() - 1; i++) {
            final MelodicPattern.Step step = pattern.step(i);
            final MelodicPattern.Step next = pattern.step(i + 1);
            if (!step.active() || next.active()) {
                continue;
            }
            if (random.nextDouble() < parameters.tension() * 0.08) {
                pattern = pattern.withStep(next.withTieFromPrevious(true));
            }
        }
        return pattern;
    }

    private int countActive(final boolean[] active) {
        int count = 0;
        for (final boolean value : active) {
            if (value) {
                count++;
            }
        }
        return count;
    }
}
