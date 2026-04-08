package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CallResponseGenerator implements MelodicGenerator {
    private static final int[] CALL_POSITIONS = {0, 2, 3, 4, 6, 7};
    private static final int[] RESPONSE_OPTIONS = {-2, -1, 0, 1, 2};

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final int half = Math.max(1, loopSteps / 2);
        final boolean[] active = new boolean[loopSteps];
        final int[] degrees = new int[loopSteps];

        active[0] = true;
        if (half < loopSteps) {
            active[half] = true;
        }
        active[loopSteps - 1] = true;

        final int callTarget = Math.max(3, Math.min(half, 3 + (int) Math.round(parameters.density() * 3)));
        while (countActive(active, 0, half) < callTarget) {
            final int candidate = CALL_POSITIONS[random.nextInt(CALL_POSITIONS.length)];
            if (candidate < half) {
                active[candidate] = true;
            }
        }

        final int[] callDegrees = buildCallDegrees(random, active, half, parameters.tension());
        System.arraycopy(callDegrees, 0, degrees, 0, half);
        buildResponse(random, active, degrees, half, loopSteps, parameters.tension());

        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps || !active[i]) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean anchor = i == 0 || i == half || i == loopSteps - 1 || i % 4 == 0;
            final int octaveOffset = random.nextDouble() < parameters.octaveActivity() * 0.3
                    ? (random.nextBoolean() ? 1 : -1)
                    : 0;
            final int pitch = context.pitchForDegree(octaveOffset, degrees[i]);
            final boolean accent = anchor || random.nextDouble() < parameters.tension() * 0.18;
            final boolean slide = !anchor && random.nextDouble() < parameters.tension() * 0.08 && i < loopSteps - 1;
            final double gate = slide ? 1.02 : (accent ? 0.90 : 0.72 + parameters.density() * 0.16);
            final int velocity = accent ? 116 : 84 + random.nextInt(18);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }
        return new MelodicPattern(steps, loopSteps);
    }

    private int[] buildCallDegrees(final Random random, final boolean[] active, final int half, final double tension) {
        final int[] degrees = new int[Math.max(1, half)];
        int previous = 0;
        for (int i = 0; i < half; i++) {
            if (!active[i]) {
                continue;
            }
            final boolean anchor = i == 0 || i % 4 == 0;
            final int maxStep = tension >= 0.55 ? 3 : 2;
            final int delta = anchor ? 0 : random.nextInt(maxStep * 2 + 1) - maxStep;
            previous = Math.max(0, Math.min(7, previous + delta));
            if (anchor && random.nextDouble() < 0.6) {
                previous = 0;
            }
            degrees[i] = previous;
        }
        return degrees;
    }

    private void buildResponse(final Random random, final boolean[] active, final int[] degrees, final int half,
                               final int loopSteps, final double tension) {
        for (int i = half; i < loopSteps; i++) {
            final int callIndex = i - half;
            if (callIndex >= half || !active[callIndex]) {
                active[i] = random.nextDouble() < tension * 0.12 && i != loopSteps - 1;
                if (active[i]) {
                    degrees[i] = Math.max(0, Math.min(7, degrees[Math.max(0, callIndex - 1)] + random.nextInt(3) - 1));
                }
                continue;
            }
            active[i] = true;
            final int responseMove = RESPONSE_OPTIONS[random.nextInt(RESPONSE_OPTIONS.length)];
            final int degree = Math.max(0, Math.min(7, degrees[callIndex] + responseMove));
            degrees[i] = i == loopSteps - 1 ? 0 : degree;
        }
    }

    private int countActive(final boolean[] active, final int startInclusive, final int endExclusive) {
        int count = 0;
        for (int i = startInclusive; i < endExclusive; i++) {
            if (active[i]) {
                count++;
            }
        }
        return count;
    }
}
