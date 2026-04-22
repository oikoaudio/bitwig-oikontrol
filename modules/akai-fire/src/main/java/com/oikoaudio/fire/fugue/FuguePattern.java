package com.oikoaudio.fire.fugue;

import com.oikoaudio.fire.melodic.MelodicPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FuguePattern {
    public static final int MAX_STEPS = 512;

    private final List<List<MelodicPattern.Step>> steps;
    private final int loopSteps;

    public FuguePattern(final List<MelodicPattern.Step> steps, final int loopSteps) {
        if (steps.size() != MAX_STEPS) {
            throw new IllegalArgumentException("FuguePattern requires exactly %d steps".formatted(MAX_STEPS));
        }
        final List<List<MelodicPattern.Step>> wrapped = new ArrayList<>(MAX_STEPS);
        for (final MelodicPattern.Step step : steps) {
            wrapped.add(step.active() ? List.of(step) : List.of());
        }
        this.steps = List.copyOf(wrapped);
        this.loopSteps = Math.max(1, Math.min(MAX_STEPS, loopSteps));
    }

    public FuguePattern(final List<List<MelodicPattern.Step>> steps, final int loopSteps, final boolean polyphonic) {
        if (steps.size() != MAX_STEPS) {
            throw new IllegalArgumentException("FuguePattern requires exactly %d steps".formatted(MAX_STEPS));
        }
        final List<List<MelodicPattern.Step>> copy = new ArrayList<>(MAX_STEPS);
        for (final List<MelodicPattern.Step> step : steps) {
            copy.add(List.copyOf(step));
        }
        this.steps = List.copyOf(copy);
        this.loopSteps = Math.max(1, Math.min(MAX_STEPS, loopSteps));
    }

    public static FuguePattern empty(final int loopSteps) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(MAX_STEPS);
        for (int i = 0; i < MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        return new FuguePattern(steps, loopSteps);
    }

    public MelodicPattern.Step step(final int index) {
        final List<MelodicPattern.Step> notes = steps.get(index);
        return notes.isEmpty() ? MelodicPattern.Step.rest(index) : notes.get(0);
    }

    public List<MelodicPattern.Step> notesAt(final int index) {
        return steps.get(index);
    }

    public int loopSteps() {
        return loopSteps;
    }

    static List<MelodicPattern.Step> emptySteps() {
        final List<MelodicPattern.Step> steps = new ArrayList<>(Collections.nCopies(MAX_STEPS,
                MelodicPattern.Step.rest(0)));
        for (int i = 0; i < MAX_STEPS; i++) {
            steps.set(i, MelodicPattern.Step.rest(i));
        }
        return steps;
    }

    static List<List<MelodicPattern.Step>> emptyPolySteps() {
        final List<List<MelodicPattern.Step>> steps = new ArrayList<>(MAX_STEPS);
        for (int i = 0; i < MAX_STEPS; i++) {
            steps.add(new ArrayList<>());
        }
        return steps;
    }
}
