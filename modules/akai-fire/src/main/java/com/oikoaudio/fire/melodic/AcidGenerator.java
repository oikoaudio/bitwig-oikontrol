package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AcidGenerator implements MelodicGenerator {
    private static final boolean[][] RHYTHM_SKELETONS = {
            mask(1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
            mask(1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1),
            mask(1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1),
            mask(1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1),
            mask(1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1)
    };

    private static final int[] LOW_RING = {0, 0, 1, 2, 0, 1, 3, 2};

    private enum Family {
        FUNDAMENTAL_ROLL,
        ROOT_ANSWER,
        OCTAVE_LEAD,
        SUPPORT_HOOK
    }

    @Override
    public MelodicPattern generate(final MelodicPhraseContext context, final GenerateParameters parameters) {
        final int loopSteps = Math.max(1, Math.min(MelodicPattern.MAX_STEPS, parameters.loopSteps()));
        final Random random = new Random(parameters.seed());
        final boolean[] active = buildActivity(loopSteps, parameters.density(), random);
        final Family family = Family.values()[random.nextInt(Family.values().length)];
        final int[] degrees = new int[loopSteps];
        final int[] octaveOffsets = new int[loopSteps];
        final boolean[] accents = new boolean[loopSteps];
        final boolean[] slides = new boolean[loopSteps];

        buildLowLine(active, degrees, accents, parameters.tension(), random);
        if (family == Family.ROOT_ANSWER || random.nextDouble() < 0.26 + parameters.tension() * 0.10) {
            injectUpperAnswer(active, degrees, octaveOffsets, accents, parameters.tension(), random);
        }
        if (family == Family.OCTAVE_LEAD || random.nextDouble() < 0.22 + parameters.octaveActivity() * 0.16) {
            injectOctaveLead(active, degrees, octaveOffsets, slides, random);
        }
        if (family == Family.SUPPORT_HOOK) {
            injectSupportHook(active, degrees, accents, random);
        }
        enforceMovement(active, degrees, octaveOffsets);

        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            if (i >= loopSteps || !activeAt(active, i)) {
                steps.add(MelodicPattern.Step.rest(i));
                continue;
            }
            final boolean anchor = isAnchorStep(i, loopSteps);
            final boolean accent = accents[i] || anchor;
            final boolean slide = slides[i] && nextActive(active, i, loopSteps);
            final int pitch = context.pitchForDegree(octaveOffsets[i], degrees[i]);
            final double gate = slide ? 1.12 : gateFor(degrees[i], accent, anchor);
            final int velocity = accent ? 116 + random.nextInt(8) : 86 + random.nextInt(14);
            steps.add(new MelodicPattern.Step(i, true, false, pitch, velocity, gate, accent, slide));
        }

        MelodicPattern pattern = new MelodicPattern(steps, loopSteps);
        pattern = addAnchorTies(pattern, active, random, parameters.tension());
        if (distinctActivePitchCount(pattern) < 3) {
            pattern = forceThirdPitch(pattern, context, active);
        }
        return pattern;
    }

    private boolean[] buildActivity(final int loopSteps, final double density, final Random random) {
        final boolean[] skeleton = RHYTHM_SKELETONS[random.nextInt(RHYTHM_SKELETONS.length)];
        final boolean[] active = new boolean[loopSteps];
        for (int i = 0; i < loopSteps; i++) {
            active[i] = skeleton[i % skeleton.length];
        }
        if (density < 0.45) {
            final List<Integer> removable = new ArrayList<>();
            for (int i = 1; i < loopSteps - 1; i++) {
                if (active[i] && !isAnchorStep(i, loopSteps) && activeAt(active, i - 1) && activeAt(active, i + 1)) {
                    removable.add(i);
                }
            }
            final int removals = Math.min(removable.size(), (int) Math.round((0.45 - density) * 6.0));
            for (int i = 0; i < removals; i++) {
                active[removable.get((i * 2) % removable.size())] = false;
            }
        } else if (density > 0.72) {
            for (int i = 1; i < loopSteps - 1; i++) {
                if (!active[i] && activeAt(active, i - 1) && activeAt(active, i + 1) && random.nextDouble() < (density - 0.72) * 0.8) {
                    active[i] = true;
                }
            }
        }
        active[0] = true;
        if (loopSteps > 1) {
            active[loopSteps - 1] = true;
        }
        return active;
    }

    private void buildLowLine(final boolean[] active, final int[] degrees, final boolean[] accents,
                              final double tension, final Random random) {
        int previousDegree = 0;
        int ringIndex = 0;
        for (int i = 0; i < active.length; i++) {
            if (!active[i]) {
                continue;
            }
            if (isAnchorStep(i, active.length)) {
                previousDegree = random.nextDouble() < 0.62 ? 0 : (i == active.length - 2 ? 2 : 0);
                accents[i] = true;
            } else if (startsCluster(active, i)) {
                previousDegree = random.nextDouble() < 0.55 ? 0 : upperNeighbor();
            } else {
                ringIndex = nearestRingIndex(previousDegree);
                final int spread = tension >= 0.65 ? 2 : 1;
                ringIndex = Math.floorMod(ringIndex + random.nextInt(spread * 2 + 1) - spread, LOW_RING.length);
                previousDegree = LOW_RING[ringIndex];
                if (previousDegree > 3 && random.nextDouble() < 0.55) {
                    previousDegree = 2;
                }
            }
            degrees[i] = previousDegree;
        }
        if (active.length > 1) {
            degrees[active.length - 1] = 0;
        }
    }

    private void injectUpperAnswer(final boolean[] active, final int[] degrees, final int[] octaveOffsets,
                                   final boolean[] accents, final double tension, final Random random) {
        final int start = answerClusterStart(active);
        if (start < 0) {
            return;
        }
        final int length = Math.min(clusterLength(active, start), tension >= 0.55 ? 4 : 3);
        final int[] motif = random.nextBoolean() ? new int[]{4, 5, 4, 2} : new int[]{4, 3, 2, 1};
        for (int i = 0; i < length; i++) {
            final int step = start + i;
            degrees[step] = motif[Math.min(i, motif.length - 1)];
            octaveOffsets[step] = i < Math.min(2, length) ? 1 : 0;
            accents[step] = i == 0;
        }
    }

    private void injectOctaveLead(final boolean[] active, final int[] degrees, final int[] octaveOffsets,
                                  final boolean[] slides, final Random random) {
        final List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < active.length - 1; i++) {
            if (!active[i] || !active[i + 1]) {
                continue;
            }
            if (isAnchorStep(i + 1, active.length) || startsCluster(active, i + 1)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        final int step = candidates.get(random.nextInt(candidates.size()));
        degrees[step] = 0;
        octaveOffsets[step] = 1;
        slides[step] = true;
    }

    private void injectSupportHook(final boolean[] active, final int[] degrees, final boolean[] accents,
                                   final Random random) {
        for (int i = 5; i < active.length - 3; i++) {
            if (clusterLength(active, i) < 3) {
                continue;
            }
            degrees[i] = 2;
            degrees[i + 1] = 4;
            degrees[i + 2] = 2;
            accents[i] = true;
            if (random.nextDouble() < 0.4 && i + 3 < active.length && active[i + 3]) {
                degrees[i + 3] = 0;
            }
            return;
        }
    }

    private void enforceMovement(final boolean[] active, final int[] degrees, final int[] octaveOffsets) {
        int distinct = distinctPitchRoles(active, degrees, octaveOffsets);
        if (distinct >= 3) {
            return;
        }
        for (int i = active.length / 2; i < active.length - 1; i++) {
            if (!active[i] || isAnchorStep(i, active.length)) {
                continue;
            }
            degrees[i] = degrees[i] == 0 ? 2 : 4;
            octaveOffsets[i] = 0;
            return;
        }
    }

    private MelodicPattern addAnchorTies(final MelodicPattern pattern, final boolean[] active,
                                         final Random random, final double tension) {
        MelodicPattern out = pattern;
        for (int i = 0; i < pattern.loopSteps() - 1; i++) {
            if (!activeAt(active, i) || activeAt(active, i + 1)) {
                continue;
            }
            if (!isAnchorStep(i, pattern.loopSteps())) {
                continue;
            }
            if (random.nextDouble() < 0.08 + tension * 0.14) {
                out = out.withStep(out.step(i + 1).withTieFromPrevious(true));
            }
        }
        return out;
    }

    private MelodicPattern forceThirdPitch(final MelodicPattern input, final MelodicPhraseContext context,
                                           final boolean[] active) {
        for (int i = input.loopSteps() / 2; i < input.loopSteps() - 1; i++) {
            if (!activeAt(active, i) || isAnchorStep(i, input.loopSteps())) {
                continue;
            }
            final int pitch = context.pitchForDegree(0, 4);
            if (!pitchExists(input, pitch)) {
                return input.withStep(input.step(i).withPitch(pitch));
            }
        }
        return input;
    }

    private boolean pitchExists(final MelodicPattern pattern, final int pitch) {
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.active() && step.pitch() != null && step.pitch() == pitch) {
                return true;
            }
        }
        return false;
    }

    private int distinctPitchRoles(final boolean[] active, final int[] degrees, final int[] octaveOffsets) {
        final List<String> roles = new ArrayList<>();
        for (int i = 0; i < active.length; i++) {
            if (!active[i]) {
                continue;
            }
            final String role = degrees[i] + ":" + octaveOffsets[i];
            if (!roles.contains(role)) {
                roles.add(role);
            }
        }
        return roles.size();
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

    private boolean startsCluster(final boolean[] active, final int step) {
        return activeAt(active, step) && !activeAt(active, step - 1);
    }

    private int clusterLength(final boolean[] active, final int start) {
        if (!activeAt(active, start)) {
            return 0;
        }
        int length = 0;
        for (int i = start; i < active.length && active[i]; i++) {
            length++;
        }
        return length;
    }

    private int answerClusterStart(final boolean[] active) {
        final int searchStart = Math.max(4, active.length / 2 - 1);
        for (int i = searchStart; i < active.length - 2; i++) {
            if (clusterLength(active, i) >= 3) {
                return i;
            }
        }
        return -1;
    }

    private boolean nextActive(final boolean[] active, final int step, final int loopSteps) {
        return step + 1 < loopSteps && active[step + 1];
    }

    private boolean activeAt(final boolean[] active, final int index) {
        return index >= 0 && index < active.length && active[index];
    }

    private boolean isAnchorStep(final int step, final int loopSteps) {
        return step == 0 || step == loopSteps - 1 || step % 4 == 0;
    }

    private double gateFor(final int degree, final boolean accent, final boolean anchor) {
        if (anchor || degree == 0) {
            return accent ? 1.04 : 0.98;
        }
        return accent ? 0.92 : 0.82;
    }

    private int nearestRingIndex(final int degree) {
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < LOW_RING.length; i++) {
            final int distance = Math.abs(LOW_RING[i] - degree);
            if (distance < bestDistance) {
                bestIndex = i;
                bestDistance = distance;
            }
        }
        return bestIndex;
    }

    private int upperNeighbor() {
        return 1;
    }

    private static boolean[] mask(final int... values) {
        final boolean[] out = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i] != 0;
        }
        return out;
    }
}
