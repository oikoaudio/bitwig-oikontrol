package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MelodicPattern {
    public static final int MAX_STEPS = 32;

    private final List<Step> steps;
    private final int loopSteps;

    public MelodicPattern(final List<Step> steps, final int loopSteps) {
        if (steps.size() != MAX_STEPS) {
            throw new IllegalArgumentException("MelodicPattern requires exactly %d steps".formatted(MAX_STEPS));
        }
        this.steps = List.copyOf(steps);
        this.loopSteps = Math.max(1, Math.min(MAX_STEPS, loopSteps));
    }

    public static MelodicPattern empty(final int loopSteps) {
        final List<Step> steps = new ArrayList<>(MAX_STEPS);
        for (int i = 0; i < MAX_STEPS; i++) {
            steps.add(Step.rest(i));
        }
        return new MelodicPattern(steps, loopSteps);
    }

    public List<Step> steps() {
        return steps;
    }

    public Step step(final int index) {
        return steps.get(index);
    }

    public int loopSteps() {
        return loopSteps;
    }

    public MelodicPattern withStep(final Step step) {
        final List<Step> copy = new ArrayList<>(steps);
        copy.set(step.index(), step);
        return new MelodicPattern(copy, loopSteps);
    }

    public MelodicPattern withLoopSteps(final int newLoopSteps) {
        return new MelodicPattern(steps, newLoopSteps);
    }

    public MelodicPattern rotated(final int amount) {
        final int normalized = Math.floorMod(amount, loopSteps);
        if (normalized == 0) {
            return this;
        }
        final List<Step> rotated = new ArrayList<>(Collections.nCopies(MAX_STEPS, Step.rest(0)));
        for (int i = 0; i < MAX_STEPS; i++) {
            rotated.set(i, Step.rest(i));
        }
        for (int i = 0; i < loopSteps; i++) {
            final int sourceIndex = Math.floorMod(i - normalized, loopSteps);
            rotated.set(i, steps.get(sourceIndex).withIndex(i));
        }
        for (int i = loopSteps; i < MAX_STEPS; i++) {
            rotated.set(i, steps.get(i).withIndex(i));
        }
        return new MelodicPattern(rotated, loopSteps);
    }

    public MelodicPattern reversed() {
        final List<Step> reversed = new ArrayList<>(Collections.nCopies(MAX_STEPS, Step.rest(0)));
        for (int i = 0; i < MAX_STEPS; i++) {
            reversed.set(i, Step.rest(i));
        }
        for (int i = 0; i < loopSteps; i++) {
            reversed.set(i, steps.get(loopSteps - 1 - i).withIndex(i));
        }
        for (int i = loopSteps; i < MAX_STEPS; i++) {
            reversed.set(i, steps.get(i).withIndex(i));
        }
        return new MelodicPattern(reversed, loopSteps);
    }

    public MelodicPattern transposed(final int semitones) {
        final List<Step> out = new ArrayList<>(MAX_STEPS);
        for (final Step step : steps) {
            if (!step.active() || step.pitch() == null) {
                out.add(step);
                continue;
            }
            out.add(step.withPitch(Math.max(0, Math.min(127, step.pitch() + semitones))));
        }
        return new MelodicPattern(out, loopSteps);
    }

    public record Step(int index, boolean active, boolean tieFromPrevious, Integer pitch, int velocity,
                       double gate, boolean accent, boolean slide) {
        public static Step rest(final int index) {
            return new Step(index, false, false, null, 96, 0.8, false, false);
        }

        public Step withIndex(final int newIndex) {
            return new Step(newIndex, active, tieFromPrevious, pitch, velocity, gate, accent, slide);
        }

        public Step withActive(final boolean newActive) {
            return new Step(index, newActive, tieFromPrevious, pitch, velocity, gate, accent, slide);
        }

        public Step withTieFromPrevious(final boolean tie) {
            return new Step(index, active, tie, pitch, velocity, gate, accent, slide);
        }

        public Step withPitch(final Integer newPitch) {
            return new Step(index, active, tieFromPrevious, newPitch, velocity, gate, accent, slide);
        }

        public Step withVelocity(final int newVelocity) {
            return new Step(index, active, tieFromPrevious, pitch, Math.max(1, Math.min(127, newVelocity)),
                    gate, accent, slide);
        }

        public Step withGate(final double newGate) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, Math.max(0.1, Math.min(1.25, newGate)),
                    accent, slide);
        }

        public Step withAccent(final boolean newAccent) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, newAccent, slide);
        }

        public Step withSlide(final boolean newSlide) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, accent, newSlide);
        }
    }
}
