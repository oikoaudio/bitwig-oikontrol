package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RollingBassGenerator implements MelodicGenerator {
    private static final int[] CELL_START_DEGREES = {0, 2, 4, 5};
    private static final int[] CELL_SHIFTS = {-2, -1, 1, 2};
    private static final int[] CELL_PATTERN = {0, 1, 1, 0, 2, 1, 3, 1};
    private static final boolean[] CELL_ACTIVITY = {true, true, true, false, true, true, true, false};

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        final int cellLength = loopSteps >= 16 ? 8 : 4;
        int cellRoot = CELL_START_DEGREES[random.nextInt(CELL_START_DEGREES.length)];
        int previousDegree = cellRoot;

        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            if (i > 0 && i % cellLength == 0) {
                cellRoot = nextCellRoot(cellRoot, parameters.tension(), random);
            }

            final int inCell = i % cellLength;
            final boolean active = activeAt(inCell, parameters.density(), random);
            if (!active) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }

            final int degree = degreeAt(inCell, cellRoot, previousDegree, parameters.tension(), random);
            previousDegree = degree;
            final boolean anchor = inCell == 0 || i == loopSteps - 1;
            final int octaveOffset = chooseOctaveOffset(inCell, parameters.octaveActivity(), random);
            final int pitch = context.pitchForDegree(octaveOffset, degree);
            final boolean accent = anchor || inCell == cellLength - 2;
            final boolean slide = !anchor && inCell == cellLength - 3 && random.nextDouble() < parameters.tension() * 0.10;
            final double gate = slide ? 1.02 : (inCell % 2 == 0 ? 0.96 : 0.88 + parameters.density() * 0.10);
            final int velocity = accent ? 114 : 86 + (inCell % 2 == 0 ? 10 : 0) + random.nextInt(6);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }
        return new MelodicPattern(steps, loopSteps);
    }

    private int nextCellRoot(final int currentRoot, final double tension, final Random random) {
        if (random.nextDouble() < 0.35 - tension * 0.10) {
            return currentRoot;
        }
        final int shift = CELL_SHIFTS[random.nextInt(tension >= 0.45 ? CELL_SHIFTS.length : CELL_SHIFTS.length - 1)];
        return clampDegree(currentRoot + shift);
    }

    private boolean activeAt(final int inCell, final double density, final Random random) {
        if (CELL_ACTIVITY[inCell]) {
            return true;
        }
        return random.nextDouble() < Math.max(0.18, density * 0.42);
    }

    private int degreeAt(final int inCell, final int cellRoot, final int previousDegree, final double tension,
                         final Random random) {
        final int pattern = CELL_PATTERN[inCell];
        if (pattern == 0) {
            return cellRoot;
        }
        if (pattern == 1) {
            final int[] offsets = tension >= 0.45 ? new int[]{1, -1, 2} : new int[]{1, -1};
            return clampDegree(cellRoot + offsets[random.nextInt(offsets.length)]);
        }
        if (pattern == 2) {
            return clampDegree(cellRoot + (random.nextBoolean() ? 2 : -2));
        }
        final int answer = random.nextDouble() < 0.55 ? previousDegree : cellRoot + 4;
        return clampDegree(answer);
    }

    private int chooseOctaveOffset(final int inCell, final double octaveActivity, final Random random) {
        final double chance = inCell == 0 ? octaveActivity * 0.08 : octaveActivity * 0.16;
        if (random.nextDouble() >= chance) {
            return 0;
        }
        return random.nextDouble() < 0.85 ? 1 : -1;
    }

    private int clampDegree(final int degree) {
        return Math.max(0, Math.min(7, degree));
    }
}
