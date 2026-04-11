package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RollingBassGenerator implements MelodicGenerator {
    private static final int[] CELL_START_DEGREES = {0, 0, 1, 2};
    private static final int[] CELL_SHIFTS = {-1, 0, 1, 2};
    private static final int[][] FAMILY_PATTERNS = {
            {0, 1, 0, 1, 2, 1, 0, 1},
            {0, 1, 0, 1, 0, 2, 0, 1},
            {0, 1, 2, 1, 0, 1, 3, 1}
    };
    private static final boolean[][] FAMILY_ACTIVITY = {
            {true, true, true, false, true, true, true, false},
            {false, true, true, true, false, true, true, true},
            {true, true, true, false, true, true, true, true}
    };

    private enum Family {
        ROOT_DRIVE,
        KICK_POCKET,
        LATE_LIFT
    }

    private String lastFamilyLabel = "";
    private int subtypeIndex = -1;

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        final int cellLength = loopSteps >= 16 ? 8 : 4;
        final Family family = chooseFamily(parameters.density(), random);
        lastFamilyLabel = familyLabel(family);
        int cellRoot = CELL_START_DEGREES[random.nextInt(CELL_START_DEGREES.length)];
        int previousDegree = cellRoot;

        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            if (i > 0 && i % cellLength == 0) {
                cellRoot = nextCellRoot(cellRoot, parameters.tension(), parameters.octaveActivity(), random);
            }

            final int inCell = i % cellLength;
            final boolean active = activeAt(inCell, family, parameters.density(), random);
            if (!active) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }

            final int degree = degreeAt(inCell, family, cellRoot, previousDegree,
                    parameters.tension(), parameters.octaveActivity(), random);
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

    @Override
    public String lastFamilyLabel() {
        return lastFamilyLabel;
    }

    @Override
    public boolean supportsSubtypeSelection() {
        return true;
    }

    @Override
    public void cycleSubtype(final int direction) {
        final int variantCount = Family.values().length + 1;
        final int next = Math.floorMod(subtypeIndex + 1 + direction, variantCount);
        subtypeIndex = next - 1;
    }

    @Override
    public String currentSubtypeLabel() {
        return subtypeIndex < 0 ? "Any" : familyLabel(Family.values()[subtypeIndex]);
    }

    private String familyLabel(final Family family) {
        return switch (family) {
            case ROOT_DRIVE -> "RootDrive";
            case KICK_POCKET -> "KickPocket";
            case LATE_LIFT -> "LateLift";
        };
    }

    private Family chooseFamily(final double density, final Random random) {
        if (subtypeIndex >= 0) {
            return Family.values()[subtypeIndex];
        }
        if (density >= 0.999) {
            final int pick = random.nextInt(10);
            if (pick < 5) {
                return Family.ROOT_DRIVE;
            }
            if (pick < 8) {
                return Family.LATE_LIFT;
            }
            return Family.KICK_POCKET;
        }
        return Family.values()[random.nextInt(Family.values().length)];
    }

    private int nextCellRoot(final int currentRoot, final double tension, final double movement, final Random random) {
        final double stayChance = Math.max(0.12, 0.62 - tension * 0.10 - movement * 0.45);
        if (random.nextDouble() < stayChance) {
            return currentRoot;
        }
        final int limit = tension + movement >= 0.55 ? CELL_SHIFTS.length : CELL_SHIFTS.length - 1;
        final int shift = CELL_SHIFTS[random.nextInt(limit)];
        return clampDegree(currentRoot + shift);
    }

    private boolean activeAt(final int inCell, final Family family, final double density, final Random random) {
        if (density >= 0.999) {
            return true;
        }
        if (FAMILY_ACTIVITY[family.ordinal()][inCell]) {
            return true;
        }
        return random.nextDouble() < Math.max(0.18, density * 0.42);
    }

    private int degreeAt(final int inCell, final Family family, final int cellRoot, final int previousDegree,
                         final double tension, final double movement, final Random random) {
        final int pattern = FAMILY_PATTERNS[family.ordinal()][inCell];
        if (pattern == 0) {
            return cellRoot;
        }
        if (pattern == 1) {
            final int[] offsets = movement >= 0.55
                    ? new int[]{1, -1, 2, -2, 3, 0}
                    : tension >= 0.45 ? new int[]{1, -1, 2, 0} : new int[]{1, -1, 0};
            return clampDegree(cellRoot + offsets[random.nextInt(offsets.length)]);
        }
        if (pattern == 2) {
            if (movement >= 0.55 && random.nextDouble() < 0.45) {
                return clampDegree(cellRoot + (family == Family.LATE_LIFT ? 4 : 3));
            }
            return clampDegree(cellRoot + (family == Family.LATE_LIFT && random.nextDouble() < 0.55 ? 3
                    : random.nextDouble() < 0.72 ? 1 : 2));
        }
        final int answer = random.nextDouble() < Math.max(0.25, 0.78 - movement * 0.40)
                ? previousDegree
                : cellRoot + (family == Family.LATE_LIFT ? 4 : movement >= 0.6 ? 3 : 2);
        return clampDegree(answer);
    }

    private int chooseOctaveOffset(final int inCell, final double octaveActivity, final Random random) {
        final double chance = inCell == 0 ? octaveActivity * 0.03 : octaveActivity * 0.07;
        if (random.nextDouble() >= chance) {
            return 0;
        }
        return random.nextDouble() < 0.92 ? -1 : 1;
    }

    private int clampDegree(final int degree) {
        return Math.max(0, Math.min(7, degree));
    }
}
