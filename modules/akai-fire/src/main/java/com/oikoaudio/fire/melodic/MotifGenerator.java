package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class MotifGenerator implements MelodicGenerator {
    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final PhraseContourLibrary.ContourBlueprint blueprint = PhraseContourLibrary.motifBlueprint(
                loopSteps, random, parameters.density(), parameters.tension());

        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        final int[] realizedDegrees = new int[loopSteps];
        Arrays.fill(realizedDegrees, Integer.MIN_VALUE);
        int previousDegree = 0;
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final PhraseContourLibrary.ContourAction action = blueprint.actions()[i];
            if (action == PhraseContourLibrary.ContourAction.REST) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final int degree = realizeDegree(action, i, realizedDegrees, previousDegree, blueprint.template(),
                    parameters.tension(), random);
            realizedDegrees[i] = degree;
            previousDegree = degree;

            final int octaveOffset = octaveOffset(action, i, loopSteps, parameters.octaveActivity(), random);
            final int pitch = context.pitchForDegree(octaveOffset, degree);
            final boolean accent = accentFor(action, i, loopSteps, random);
            final boolean slide = slideFor(blueprint.actions(), i, loopSteps, parameters.tension(), random);
            final double gate = gateFor(action, slide, accent, parameters.density());
            final int velocity = accent ? 114 + random.nextInt(8) : 84 + random.nextInt(16);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }

        MelodicPattern pattern = new MelodicPattern(steps, loopSteps);
        pattern = maybeAddTies(pattern, blueprint.actions(), parameters.tension(), random);
        return enforceMotifContrast(pattern, context, blueprint);
    }

    private int realizeDegree(final PhraseContourLibrary.ContourAction action, final int step,
                              final int[] realizedDegrees, final int previousDegree,
                              final PhraseContourLibrary.ContourTemplate template, final double tension,
                              final Random random) {
        return switch (action) {
            case ANCHOR -> choose(template.anchorDegrees(), random);
            case CADENCE -> random.nextDouble() < 0.7 ? 0 : nearestDegree(previousDegree, template.cadenceDegrees());
            case REPEAT -> copiedDegree(step, realizedDegrees, previousDegree);
            case PICKUP -> nearestDegree(previousDegree + 1, template.coreDegrees());
            case NEIGHBOR -> nearestDegree(previousDegree + (random.nextBoolean() ? 1 : -1), template.coreDegrees());
            case STEP -> nearestDegree(previousDegree + (random.nextBoolean() ? 1 : -1), movementPalette(template, tension));
            case LEAP -> nearestDegree(previousDegree + (random.nextBoolean() ? 2 : -2), movementPalette(template, tension));
            case ORNAMENT -> choose(colorPalette(template, tension), random);
            case OCTAVE -> copiedDegree(step, realizedDegrees, previousDegree);
            case REST -> previousDegree;
        };
    }

    private int copiedDegree(final int step, final int[] realizedDegrees, final int previousDegree) {
        final int[] candidates = {step - 8, step - 4, step - 2, step - 1};
        for (final int index : candidates) {
            if (index >= 0 && realizedDegrees[index] != Integer.MIN_VALUE) {
                return realizedDegrees[index];
            }
        }
        return previousDegree;
    }

    private int[] movementPalette(final PhraseContourLibrary.ContourTemplate template, final double tension) {
        return tension >= 0.6 ? append(template.coreDegrees(), template.colorDegrees()) : template.coreDegrees();
    }

    private int[] colorPalette(final PhraseContourLibrary.ContourTemplate template, final double tension) {
        return tension >= 0.55 ? append(template.coreDegrees(), template.colorDegrees()) : template.colorDegrees();
    }

    private int octaveOffset(final PhraseContourLibrary.ContourAction action, final int step, final int loopSteps,
                             final double octaveActivity, final Random random) {
        if (action == PhraseContourLibrary.ContourAction.OCTAVE) {
            return random.nextBoolean() ? 1 : -1;
        }
        if (step == loopSteps / 2 && random.nextDouble() < octaveActivity * 0.18) {
            return 1;
        }
        return 0;
    }

    private boolean accentFor(final PhraseContourLibrary.ContourAction action, final int step, final int loopSteps,
                              final Random random) {
        if (action == PhraseContourLibrary.ContourAction.ANCHOR
                || action == PhraseContourLibrary.ContourAction.CADENCE) {
            return true;
        }
        if (action == PhraseContourLibrary.ContourAction.PICKUP) {
            return random.nextDouble() < 0.40;
        }
        return step == loopSteps / 2 && random.nextDouble() < 0.45;
    }

    private boolean slideFor(final PhraseContourLibrary.ContourAction[] actions, final int step, final int loopSteps,
                             final double tension, final Random random) {
        if (step >= loopSteps - 1) {
            return false;
        }
        final PhraseContourLibrary.ContourAction current = actions[step];
        final PhraseContourLibrary.ContourAction next = actions[step + 1];
        if (next == PhraseContourLibrary.ContourAction.REST) {
            return false;
        }
        final boolean resolvesForward = current == PhraseContourLibrary.ContourAction.PICKUP
                || current == PhraseContourLibrary.ContourAction.NEIGHBOR;
        final boolean landing = next == PhraseContourLibrary.ContourAction.ANCHOR
                || next == PhraseContourLibrary.ContourAction.CADENCE;
        return resolvesForward && landing && random.nextDouble() < 0.12 + tension * 0.10;
    }

    private double gateFor(final PhraseContourLibrary.ContourAction action, final boolean slide,
                           final boolean accent, final double density) {
        if (slide) {
            return 1.05;
        }
        return switch (action) {
            case ANCHOR, CADENCE -> accent ? 0.94 : 0.88;
            case ORNAMENT -> 0.62 + density * 0.08;
            case PICKUP, NEIGHBOR -> 0.72 + density * 0.08;
            default -> accent ? 0.90 : 0.76 + density * 0.14;
        };
    }

    private MelodicPattern maybeAddTies(final MelodicPattern input, final PhraseContourLibrary.ContourAction[] actions,
                                        final double tension, final Random random) {
        MelodicPattern pattern = input;
        for (int i = 0; i < input.loopSteps() - 1; i++) {
            final MelodicPattern.Step step = pattern.step(i);
            final MelodicPattern.Step next = pattern.step(i + 1);
            if (!step.active() || next.active()) {
                continue;
            }
            if (actions[i] != PhraseContourLibrary.ContourAction.ANCHOR
                    && actions[i] != PhraseContourLibrary.ContourAction.CADENCE) {
                continue;
            }
            if (random.nextDouble() < tension * 0.08) {
                pattern = pattern.withStep(next.withTieFromPrevious(true));
            }
        }
        return pattern;
    }

    private MelodicPattern enforceMotifContrast(final MelodicPattern input, final MelodicPhraseContext context,
                                                final PhraseContourLibrary.ContourBlueprint blueprint) {
        if (distinctActivePitchCount(input) >= 2) {
            return input;
        }
        final int[] palette = append(append(blueprint.template().anchorDegrees(), blueprint.template().coreDegrees()),
                append(blueprint.template().colorDegrees(), blueprint.template().cadenceDegrees()));
        for (int i = input.loopSteps() / 2; i < input.loopSteps() - 1; i++) {
            final PhraseContourLibrary.ContourAction action = blueprint.actions()[i];
            if (action != PhraseContourLibrary.ContourAction.NEIGHBOR
                    && action != PhraseContourLibrary.ContourAction.STEP
                    && action != PhraseContourLibrary.ContourAction.LEAP
                    && action != PhraseContourLibrary.ContourAction.ORNAMENT) {
                continue;
            }
            final MelodicPattern.Step step = input.step(i);
            if (!step.active()) {
                continue;
            }
            final int pitch = context.pitchForDegree(0, nearestDegree(2, palette));
            if (pitch != step.pitch()) {
                return input.withStep(step.withPitch(pitch));
            }
        }
        return input;
    }

    private int distinctActivePitchCount(final MelodicPattern pattern) {
        final List<Integer> pitches = new ArrayList<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.active() && step.pitch() != null && !pitches.contains(step.pitch())) {
                pitches.add(step.pitch());
            }
        }
        return pitches.size();
    }

    private int nearestDegree(final int target, final int[] palette) {
        int best = palette[0];
        int bestDistance = Math.abs(best - target);
        for (final int degree : palette) {
            final int distance = Math.abs(degree - target);
            if (distance < bestDistance) {
                best = degree;
                bestDistance = distance;
            }
        }
        return best;
    }

    private int choose(final int[] source, final Random random) {
        return source[random.nextInt(source.length)];
    }

    private int[] append(final int[] left, final int[] right) {
        final int[] merged = new int[left.length + right.length];
        System.arraycopy(left, 0, merged, 0, left.length);
        System.arraycopy(right, 0, merged, left.length, right.length);
        return merged;
    }
}
