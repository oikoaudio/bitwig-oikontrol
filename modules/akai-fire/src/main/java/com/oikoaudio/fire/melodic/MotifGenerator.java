package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class MotifGenerator implements MelodicGenerator {
    private static final int[][] RHYTHM_CELLS = {
            {0, 2, 3, 5},
            {0, 3, 4, 6},
            {1, 2, 4, 6},
            {0, 2, 5, 6},
            {1, 3, 5, 6}
    };

    private static final int[][] MOTION_CELLS = {
            {1, -1, 2},
            {1, 1, -1},
            {2, -1, -1},
            {-1, 2, -1},
            {1, -2, 1}
    };

    private static final int[] START_DEGREES = {0, 2, 4};
    private static final int[] CADENCE_DEGREES = {0, 2, 4};

    private enum MotifFamily {
        REPEAT_THEN_TAIL,
        SEQUENCE_REPLY,
        TRUNCATE_EXTEND,
        HOOK_RETURN
    }

    private String lastFamilyLabel = "";

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final int sectionLength = phraseSectionLength(loopSteps);
        final int[] baseRhythm = projectPositions(RHYTHM_CELLS[random.nextInt(RHYTHM_CELLS.length)], sectionLength);
        final int[] baseCell = buildBaseCell(baseRhythm.length, parameters.tension(), random);
        final MotifFamily family = MotifFamily.values()[random.nextInt(MotifFamily.values().length)];
        lastFamilyLabel = familyLabel(family);

        final boolean[] active = new boolean[loopSteps];
        final int[] degrees = new int[loopSteps];
        final int[] octaves = new int[loopSteps];
        Arrays.fill(degrees, Integer.MIN_VALUE);

        final int sectionCount = Math.max(1, (int) Math.ceil(loopSteps / (double) sectionLength));
        for (int section = 0; section < sectionCount; section++) {
            final int start = section * sectionLength;
            if (start >= loopSteps) {
                break;
            }
            final int available = Math.min(sectionLength, loopSteps - start);
            final int[] positions = varyRhythm(baseRhythm, available, section, sectionCount,
                    family, parameters.density(), random);
            final int[] cellDegrees = varyDegrees(baseCell, section, sectionCount, family, parameters.tension(), random);
            writeSection(active, degrees, octaves, start, available, positions, cellDegrees,
                    section, sectionCount, family, parameters.octaveActivity(), random);
        }

        enforcePhraseCadence(active, degrees, loopSteps);
        ensureMotifIdentity(active, degrees, loopSteps, random);

        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps || !active[i]) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final int pitch = context.pitchForDegree(octaves[i], degrees[i]);
            final boolean sectionStart = i % sectionLength == firstActiveOffset(active, i, sectionLength);
            final boolean cadence = i == lastActiveIndex(active, loopSteps);
            final boolean accent = cadence || sectionStart;
            final boolean slide = shouldSlide(active, degrees, i, loopSteps, parameters.tension(), random);
            final double gate = gateFor(accent, cadence, slide, parameters.density());
            final int velocity = accent ? 112 + random.nextInt(8) : 86 + random.nextInt(18);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }
        return new MelodicPattern(steps, loopSteps);
    }

    @Override
    public String lastFamilyLabel() {
        return lastFamilyLabel;
    }

    private String familyLabel(final MotifFamily family) {
        return switch (family) {
            case REPEAT_THEN_TAIL -> "RepeatTail";
            case SEQUENCE_REPLY -> "Sequence";
            case TRUNCATE_EXTEND -> "TruncExtend";
            case HOOK_RETURN -> "HookReturn";
        };
    }

    private int phraseSectionLength(final int loopSteps) {
        if (loopSteps >= 16) {
            return 8;
        }
        if (loopSteps >= 8) {
            return 4;
        }
        return Math.max(2, loopSteps);
    }

    private int[] buildBaseCell(final int noteCount, final double tension, final Random random) {
        final int[] cell = new int[Math.max(2, noteCount)];
        cell[0] = START_DEGREES[random.nextInt(START_DEGREES.length)];
        final int[] motion = MOTION_CELLS[random.nextInt(MOTION_CELLS.length)];
        for (int i = 1; i < cell.length; i++) {
            int delta = motion[(i - 1) % motion.length];
            if (tension >= 0.55 && random.nextDouble() < 0.25) {
                delta += delta > 0 ? 1 : -1;
            }
            cell[i] = clampDegree(cell[i - 1] + delta);
        }
        if (distinctCount(cell) < 3 && cell.length >= 3) {
            cell[cell.length - 1] = clampDegree(cell[cell.length - 2] + 2);
        }
        return cell;
    }

    private int[] varyDegrees(final int[] baseCell, final int section, final int sectionCount,
                              final MotifFamily family, final double tension, final Random random) {
        final int[] variant = Arrays.copyOf(baseCell, baseCell.length);
        if (section == 0) {
            return variant;
        }
        switch (family) {
            case REPEAT_THEN_TAIL -> {
                variant[variant.length - 1] = cadenceDegree(random);
                if (variant.length > 2) {
                    variant[variant.length - 2] = clampDegree(variant[variant.length - 2] + (random.nextBoolean() ? 1 : -1));
                }
            }
            case SEQUENCE_REPLY -> {
                final int shift = random.nextBoolean() ? 1 : -1;
                for (int i = 0; i < variant.length; i++) {
                    variant[i] = clampDegree(variant[i] + shift);
                }
                variant[variant.length - 1] = cadenceDegree(random);
            }
            case TRUNCATE_EXTEND -> {
                if (variant.length > 2) {
                    variant[variant.length - 2] = variant[Math.max(0, variant.length - 3)];
                }
                variant[variant.length - 1] = clampDegree(variant[variant.length - 1] + (tension >= 0.5 ? 2 : 1));
                if (section == sectionCount - 1) {
                    variant[variant.length - 1] = cadenceDegree(random);
                }
            }
            case HOOK_RETURN -> {
                for (int i = Math.max(2, variant.length / 2); i < variant.length; i++) {
                    final int mirrored = Math.max(0, variant.length - 1 - i);
                    variant[i] = clampDegree(variant[mirrored] + (i == variant.length - 1 ? -1 : 1));
                }
                if (section == sectionCount - 1) {
                    variant[variant.length - 1] = cadenceDegree(random);
                }
            }
        }
        if (section == sectionCount - 1) {
            variant[variant.length - 1] = cadenceDegree(random);
        }
        return variant;
    }

    private int[] varyRhythm(final int[] baseRhythm, final int available, final int section, final int sectionCount,
                             final MotifFamily family, final double density, final Random random) {
        int[] positions = fitPositions(baseRhythm, available);
        if (section > 0) {
            switch (family) {
                case REPEAT_THEN_TAIL -> positions = moveLastHitLater(positions, available);
                case SEQUENCE_REPLY -> positions = nudgeMiddleHit(positions, available, random.nextBoolean() ? 1 : -1);
                case TRUNCATE_EXTEND -> positions = swapInteriorForTail(positions, available);
                case HOOK_RETURN -> positions = returnWithPickup(positions, available);
            }
        }
        if (density < 0.35 && positions.length > 3) {
            positions = removeInteriorHit(positions);
        } else if (density > 0.7 && positions.length < Math.min(5, available)) {
            positions = maybeAddTailHit(positions, available);
        }
        if (section == sectionCount - 1) {
            positions[positions.length - 1] = Math.max(positions[positions.length - 1], available - 1);
        }
        return uniqueSortedPositions(positions, available);
    }

    private void writeSection(final boolean[] active, final int[] degrees, final int[] octaves,
                              final int start, final int available, final int[] positions, final int[] cellDegrees,
                              final int section, final int sectionCount, final MotifFamily family,
                              final double octaveActivity, final Random random) {
        final int noteCount = Math.min(positions.length, cellDegrees.length);
        for (int i = 0; i < noteCount; i++) {
            final int localStep = positions[i];
            if (localStep < 0 || localStep >= available) {
                continue;
            }
            final int absoluteStep = start + localStep;
            active[absoluteStep] = true;
            degrees[absoluteStep] = cellDegrees[i];
            octaves[absoluteStep] = octaveFor(section, sectionCount, family, i, noteCount, octaveActivity, random);
        }
    }

    private int octaveFor(final int section, final int sectionCount, final MotifFamily family, final int noteIndex,
                          final int noteCount, final double octaveActivity, final Random random) {
        if (section == sectionCount - 1
                && family == MotifFamily.SEQUENCE_REPLY
                && noteIndex == Math.max(1, noteCount - 2)
                && random.nextDouble() < octaveActivity * 0.18) {
            return 1;
        }
        if (family == MotifFamily.HOOK_RETURN
                && noteIndex == 0
                && section > 0
                && random.nextDouble() < octaveActivity * 0.14) {
            return 1;
        }
        return 0;
    }

    private void enforcePhraseCadence(final boolean[] active, final int[] degrees, final int loopSteps) {
        final int lastActive = lastActiveIndex(active, loopSteps);
        if (lastActive < 0) {
            active[0] = true;
            degrees[0] = 0;
            return;
        }
        degrees[lastActive] = cadenceDegree(new Random(loopSteps * 31L + lastActive));
    }

    private void ensureMotifIdentity(final boolean[] active, final int[] degrees, final int loopSteps, final Random random) {
        if (distinctActiveDegreeCount(active, degrees, loopSteps) < 3) {
            for (int i = loopSteps / 2; i < loopSteps; i++) {
                if (!active[i]) {
                    continue;
                }
                degrees[i] = degrees[i] >= 3 ? 1 : 4;
                break;
            }
        }
        final int half = Math.max(1, loopSteps / 2);
        if (!hasRepeatedCell(active, degrees, half, loopSteps)) {
            final int[] source = collectDegrees(active, degrees, 0, half);
            int sourceIndex = 0;
            for (int i = half; i < loopSteps && sourceIndex < source.length; i++) {
                if (!active[i]) {
                    continue;
                }
                degrees[i] = source[sourceIndex++];
                if (sourceIndex >= 2 && random.nextDouble() < 0.25) {
                    break;
                }
            }
            final int lastActive = lastActiveIndex(active, loopSteps);
            if (lastActive >= 0) {
                degrees[lastActive] = cadenceDegree(random);
            }
        }
    }

    private boolean hasRepeatedCell(final boolean[] active, final int[] degrees, final int half, final int loopSteps) {
        final int[] first = collectDegrees(active, degrees, 0, half);
        final int[] second = collectDegrees(active, degrees, half, loopSteps);
        if (first.length < 2 || second.length < 2) {
            return false;
        }
        final int max = Math.min(first.length, second.length);
        int matches = 0;
        for (int i = 0; i < max; i++) {
            if (first[i] == second[i]) {
                matches++;
            }
        }
        return matches >= 2;
    }

    private int[] collectDegrees(final boolean[] active, final int[] degrees, final int startInclusive,
                                 final int endExclusive) {
        final List<Integer> out = new ArrayList<>();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (active[i]) {
                out.add(degrees[i]);
            }
        }
        final int[] array = new int[out.size()];
        for (int i = 0; i < out.size(); i++) {
            array[i] = out.get(i);
        }
        return array;
    }

    private int[] projectPositions(final int[] template, final int sectionLength) {
        final int[] projected = new int[template.length];
        for (int i = 0; i < template.length; i++) {
            projected[i] = Math.min(sectionLength - 1,
                    (int) Math.round(template[i] * ((sectionLength - 1) / 7.0)));
        }
        return uniqueSortedPositions(projected, sectionLength);
    }

    private int[] fitPositions(final int[] positions, final int available) {
        final int[] fitted = new int[positions.length];
        for (int i = 0; i < positions.length; i++) {
            fitted[i] = Math.max(0, Math.min(available - 1, positions[i]));
        }
        return uniqueSortedPositions(fitted, available);
    }

    private int[] moveLastHitLater(final int[] positions, final int available) {
        final int[] moved = Arrays.copyOf(positions, positions.length);
        moved[moved.length - 1] = Math.min(available - 1, moved[moved.length - 1] + 1);
        return moved;
    }

    private int[] nudgeMiddleHit(final int[] positions, final int available, final int delta) {
        if (positions.length < 3) {
            return positions;
        }
        final int[] moved = Arrays.copyOf(positions, positions.length);
        final int index = positions.length / 2;
        moved[index] = Math.max(0, Math.min(available - 1, moved[index] + delta));
        return moved;
    }

    private int[] swapInteriorForTail(final int[] positions, final int available) {
        if (positions.length < 3) {
            return positions;
        }
        final int[] moved = Arrays.copyOf(positions, positions.length);
        moved[positions.length - 2] = Math.max(moved[positions.length - 3] + 1, available - 2);
        moved[positions.length - 1] = available - 1;
        return moved;
    }

    private int[] returnWithPickup(final int[] positions, final int available) {
        final int[] moved = Arrays.copyOf(positions, positions.length);
        if (moved[0] > 0) {
            moved[0] = moved[0] - 1;
        }
        moved[moved.length - 1] = Math.min(available - 1, moved[moved.length - 1] + 1);
        return moved;
    }

    private int[] removeInteriorHit(final int[] positions) {
        final int[] reduced = new int[positions.length - 1];
        int target = Math.max(1, positions.length - 2);
        for (int i = 0, out = 0; i < positions.length; i++) {
            if (i == target) {
                continue;
            }
            reduced[out++] = positions[i];
        }
        return reduced;
    }

    private int[] maybeAddTailHit(final int[] positions, final int available) {
        final int candidate = Math.max(0, available - 2);
        for (final int position : positions) {
            if (position == candidate) {
                return positions;
            }
        }
        final int[] expanded = Arrays.copyOf(positions, positions.length + 1);
        expanded[expanded.length - 1] = candidate;
        return expanded;
    }

    private int[] uniqueSortedPositions(final int[] raw, final int available) {
        return Arrays.stream(raw)
                .map(value -> Math.max(0, Math.min(available - 1, value)))
                .distinct()
                .sorted()
                .toArray();
    }

    private boolean shouldSlide(final boolean[] active, final int[] degrees, final int step, final int loopSteps,
                                final double tension, final Random random) {
        if (step >= loopSteps - 1 || !active[step + 1]) {
            return false;
        }
        final int interval = Math.abs(degrees[step + 1] - degrees[step]);
        return interval == 1 && random.nextDouble() < 0.08 + tension * 0.08;
    }

    private double gateFor(final boolean accent, final boolean cadence, final boolean slide, final double density) {
        if (slide) {
            return 1.02;
        }
        if (cadence) {
            return 0.95;
        }
        if (accent) {
            return 0.88;
        }
        return 0.72 + density * 0.12;
    }

    private int firstActiveOffset(final boolean[] active, final int step, final int sectionLength) {
        final int start = step - (step % sectionLength);
        final int end = Math.min(active.length, start + sectionLength);
        for (int i = start; i < end; i++) {
            if (active[i]) {
                return i % sectionLength;
            }
        }
        return 0;
    }

    private int lastActiveIndex(final boolean[] active, final int loopSteps) {
        for (int i = loopSteps - 1; i >= 0; i--) {
            if (active[i]) {
                return i;
            }
        }
        return -1;
    }

    private int distinctActiveDegreeCount(final boolean[] active, final int[] degrees, final int loopSteps) {
        final List<Integer> distinct = new ArrayList<>();
        for (int i = 0; i < loopSteps; i++) {
            if (!active[i] || distinct.contains(degrees[i])) {
                continue;
            }
            distinct.add(degrees[i]);
        }
        return distinct.size();
    }

    private int distinctCount(final int[] values) {
        return (int) Arrays.stream(values).distinct().count();
    }

    private int cadenceDegree(final Random random) {
        return CADENCE_DEGREES[random.nextInt(CADENCE_DEGREES.length)];
    }

    private int clampDegree(final int degree) {
        return Math.max(0, Math.min(7, degree));
    }
}
