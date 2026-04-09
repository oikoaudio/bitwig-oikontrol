package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CallResponseGenerator implements MelodicGenerator {
    private static final int[] CALL_POSITIONS = {0, 2, 3, 4, 6, 7};
    private static final int[] ANCHOR_DEGREES = {0, 2, 4};

    private enum ResponseTransform {
        DOWN_ANSWER,
        UP_ANSWER,
        INVERT_AROUND_ROOT,
        CADENTIAL_REPLY
    }

    private String lastFamilyLabel = "";

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final int half = Math.max(1, loopSteps / 2);
        final boolean[] active = new boolean[loopSteps];
        final int[] degrees = new int[loopSteps];
        final ResponseTransform transform = ResponseTransform.values()[random.nextInt(ResponseTransform.values().length)];
        lastFamilyLabel = transformLabel(transform);

        buildCall(active, degrees, half, parameters.density(), parameters.tension(), random);
        buildResponse(active, degrees, half, loopSteps, transform, parameters.density(), parameters.tension(), random);

        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps || !active[i]) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean anchor = i == 0 || i == half || i == loopSteps - 1 || i % 4 == 0;
            final boolean responseSide = i >= half;
            final int octaveOffset = responseSide && random.nextDouble() < parameters.octaveActivity() * 0.16
                    ? 1
                    : random.nextDouble() < parameters.octaveActivity() * 0.12 ? -1 : 0;
            final int pitch = context.pitchForDegree(octaveOffset, degrees[i]);
            final boolean accent = anchor || (responseSide && i % 4 == 2);
            final boolean slide = !anchor && responseSide && i < loopSteps - 1
                    && active[i + 1] && random.nextDouble() < parameters.tension() * 0.08;
            final double gate = slide ? 1.02 : (responseSide ? 0.80 : 0.88) + parameters.density() * 0.10;
            final int velocity = accent ? 114 : 86 + random.nextInt(12);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }
        return new MelodicPattern(steps, loopSteps);
    }

    @Override
    public String lastFamilyLabel() {
        return lastFamilyLabel;
    }

    private String transformLabel(final ResponseTransform transform) {
        return switch (transform) {
            case DOWN_ANSWER -> "DownAnswer";
            case UP_ANSWER -> "UpAnswer";
            case INVERT_AROUND_ROOT -> "Invert";
            case CADENTIAL_REPLY -> "Cadential";
        };
    }

    private void buildCall(final boolean[] active, final int[] degrees, final int half, final double density,
                           final double tension, final Random random) {
        active[0] = true;
        if (half > 1) {
            active[Math.max(1, half - 1)] = true;
        }
        final int callTarget = Math.max(3, Math.min(half, 3 + (int) Math.round(density * 3)));
        while (countActive(active, 0, half) < callTarget) {
            final int candidate = CALL_POSITIONS[random.nextInt(CALL_POSITIONS.length)];
            if (candidate < half) {
                active[candidate] = true;
            }
        }

        int previous = 0;
        for (int i = 0; i < half; i++) {
            if (!active[i]) {
                continue;
            }
            if (i == 0 || i % 4 == 0) {
                previous = random.nextDouble() < 0.52 ? 0 : ANCHOR_DEGREES[random.nextInt(ANCHOR_DEGREES.length)];
            } else {
                final int delta = chooseCallDelta(tension, random);
                previous = clampDegree(previous + delta);
            }
            degrees[i] = previous;
        }
        ensureCallVariety(active, degrees, half);
    }

    private void buildResponse(final boolean[] active, final int[] degrees, final int half, final int loopSteps,
                               final ResponseTransform transform, final double density, final double tension,
                               final Random random) {
        if (half < loopSteps) {
            active[half] = true;
        }
        active[loopSteps - 1] = true;

        for (int callIndex = 0; callIndex < half && half + callIndex < loopSteps; callIndex++) {
            final int responseIndex = half + callIndex;
            if (!active[callIndex]) {
                if (responseIndex != loopSteps - 1 && random.nextDouble() < tension * 0.08) {
                    active[responseIndex] = true;
                    degrees[responseIndex] = clampDegree(degrees[Math.max(0, responseIndex - 1)] + (random.nextBoolean() ? 1 : -1));
                }
                continue;
            }
            final boolean keepHit = callIndex == 0
                    || callIndex == half - 1
                    || random.nextDouble() < 0.82 + density * 0.10;
            active[responseIndex] = keepHit || responseIndex == loopSteps - 1;
            if (!active[responseIndex]) {
                continue;
            }
            final int callDegree = degrees[callIndex];
            degrees[responseIndex] = responseIndex == loopSteps - 1
                    ? 0
                    : transformDegree(callDegree, callIndex, half, transform, random);
        }

        ensureResponseContrast(active, degrees, half, loopSteps, transform);
    }

    private int chooseCallDelta(final double tension, final Random random) {
        final int[] options = tension >= 0.45 ? new int[]{-3, -2, -1, 1, 2, 3} : new int[]{-2, -1, 1, 2};
        return options[random.nextInt(options.length)];
    }

    private int transformDegree(final int callDegree, final int callIndex, final int half,
                                final ResponseTransform transform, final Random random) {
        return switch (transform) {
            case DOWN_ANSWER -> clampDegree(callDegree - (callIndex % 3 == 1 ? 2 : 1));
            case UP_ANSWER -> clampDegree(callDegree + (callIndex % 3 == 1 ? 2 : 1));
            case INVERT_AROUND_ROOT -> clampDegree(Math.abs(callDegree - 4));
            case CADENTIAL_REPLY -> callIndex >= half - 2
                    ? clampDegree(callDegree - 2)
                    : clampDegree(callDegree + (random.nextBoolean() ? -1 : 1));
        };
    }

    private void ensureCallVariety(final boolean[] active, final int[] degrees, final int half) {
        if (distinctDegrees(active, degrees, 0, half) >= 2) {
            return;
        }
        for (int i = half - 1; i >= 1; i--) {
            if (!active[i]) {
                continue;
            }
            degrees[i] = degrees[i] >= 3 ? 1 : 4;
            return;
        }
    }

    private void ensureResponseContrast(final boolean[] active, final int[] degrees, final int half,
                                        final int loopSteps, final ResponseTransform transform) {
        if (distinctDegrees(active, degrees, 0, loopSteps) < 2) {
            for (int i = half; i < loopSteps - 1; i++) {
                if (!active[i]) {
                    continue;
                }
                degrees[i] = degrees[i] >= 3 ? 1 : 4;
                return;
            }
        }
        if (sameHalfContour(active, degrees, half, loopSteps)) {
            for (int i = half; i < loopSteps - 1; i++) {
                if (!active[i]) {
                    continue;
                }
                degrees[i] = clampDegree(degrees[i] + (transform == ResponseTransform.DOWN_ANSWER ? -1 : 1));
                return;
            }
        }
    }

    private boolean sameHalfContour(final boolean[] active, final int[] degrees, final int half, final int loopSteps) {
        for (int i = 0; i < half && half + i < loopSteps; i++) {
            if (active[i] != active[half + i]) {
                return false;
            }
            if (active[i] && degrees[i] != degrees[half + i]) {
                return false;
            }
        }
        return true;
    }

    private int distinctDegrees(final boolean[] active, final int[] degrees, final int startInclusive,
                                final int endExclusive) {
        final List<Integer> distinct = new ArrayList<>();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (!active[i] || distinct.contains(degrees[i])) {
                continue;
            }
            distinct.add(degrees[i]);
        }
        return distinct.size();
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

    private int clampDegree(final int degree) {
        return Math.max(0, Math.min(7, degree));
    }
}
