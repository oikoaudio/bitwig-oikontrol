package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.sequence.RecurrencePattern;

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
            if (!step.active() || (step.pitch() == null && step.alternatePitch() == null)) {
                out.add(step);
                continue;
            }
            Step updated = step;
            if (step.pitch() != null) {
                updated = updated.withPitch(Math.max(0, Math.min(127, step.pitch() + semitones)));
            }
            if (step.alternatePitch() != null) {
                updated = updated.withAlternatePitch(Math.max(0, Math.min(127, step.alternatePitch() + semitones)));
            }
            out.add(updated);
        }
        return new MelodicPattern(out, loopSteps);
    }

    public record Step(int index, boolean active, boolean tieFromPrevious, Integer pitch, int velocity,
                       double gate, double chance, boolean accent, boolean slide, int recurrenceLength, int recurrenceMask,
                       Integer alternatePitch, int alternateVelocity, double alternateGate, double alternateChance,
                       boolean alternateAccent, boolean alternateSlide, int alternateRecurrenceLength, int alternateRecurrenceMask) {
        public Step {
            final RecurrencePattern recurrence = RecurrencePattern.of(recurrenceLength, recurrenceMask);
            recurrenceLength = recurrence.length();
            recurrenceMask = recurrence.mask();
            chance = Math.max(0.0, Math.min(1.0, chance));
            if (alternatePitch == null) {
                alternateVelocity = velocity;
                alternateGate = gate;
                alternateChance = chance;
                alternateAccent = false;
                alternateSlide = false;
                alternateRecurrenceLength = 0;
                alternateRecurrenceMask = 0;
            } else {
                final RecurrencePattern alternateRecurrence = RecurrencePattern.of(alternateRecurrenceLength, alternateRecurrenceMask);
                alternateRecurrenceLength = alternateRecurrence.length();
                alternateRecurrenceMask = alternateRecurrence.mask();
                alternateChance = Math.max(0.0, Math.min(1.0, alternateChance));
                final int[] normalized = normalizeSameStepRecurrence(
                        recurrenceLength, recurrenceMask,
                        alternateRecurrenceLength, alternateRecurrenceMask);
                recurrenceLength = normalized[0];
                recurrenceMask = normalized[1];
                alternateRecurrenceLength = normalized[2];
                alternateRecurrenceMask = normalized[3];
            }
        }

        public Step(final int index, final boolean active, final boolean tieFromPrevious, final Integer pitch,
                    final int velocity, final double gate, final boolean accent, final boolean slide) {
            this(index, active, tieFromPrevious, pitch, velocity, gate, 1.0, accent, slide, 0, 0,
                    null, velocity, gate, 1.0, false, false, 0, 0);
        }

        public Step(final int index, final boolean active, final boolean tieFromPrevious, final Integer pitch,
                    final int velocity, final double gate, final double chance, final boolean accent,
                    final boolean slide, final int recurrenceLength, final int recurrenceMask) {
            this(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide, recurrenceLength, recurrenceMask,
                    null, velocity, gate, chance, false, false, 0, 0);
        }

        public static Step rest(final int index) {
            return new Step(index, false, false, null, 96, 0.8, 1.0, false, false, 0, 0,
                    null, 96, 0.8, 1.0, false, false, 0, 0);
        }

        public Step withIndex(final int newIndex) {
            return new Step(newIndex, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withActive(final boolean newActive) {
            return new Step(index, newActive, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withTieFromPrevious(final boolean tie) {
            return new Step(index, active, tie, pitch, velocity, gate, chance, accent, slide, recurrenceLength, recurrenceMask,
                    alternatePitch, alternateVelocity, alternateGate, alternateChance, alternateAccent, alternateSlide,
                    alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withPitch(final Integer newPitch) {
            return new Step(index, active, tieFromPrevious, newPitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withVelocity(final int newVelocity) {
            return new Step(index, active, tieFromPrevious, pitch, Math.max(1, Math.min(127, newVelocity)),
                    gate, chance, accent, slide, recurrenceLength, recurrenceMask,
                    alternatePitch, alternateVelocity, alternateGate, alternateChance, alternateAccent, alternateSlide,
                    alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withGate(final double newGate) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, Math.max(0.1, Math.min(1.25, newGate)),
                    chance, accent, slide, recurrenceLength, recurrenceMask,
                    alternatePitch, alternateVelocity, alternateGate, alternateChance, alternateAccent, alternateSlide,
                    alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withChance(final double newChance) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, newChance,
                    accent, slide, recurrenceLength, recurrenceMask,
                    alternatePitch, alternateVelocity, alternateGate, alternateChance, alternateAccent, alternateSlide,
                    alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withAccent(final boolean newAccent) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, newAccent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withSlide(final boolean newSlide) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, newSlide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withRecurrence(final int length, final int mask) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide, length, mask,
                    alternatePitch, alternateVelocity, alternateGate, alternateChance, alternateAccent, alternateSlide,
                    alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public int bitwigRecurrenceLength() {
            return RecurrencePattern.of(recurrenceLength, recurrenceMask).bitwigLength();
        }

        public int bitwigRecurrenceMask() {
            return RecurrencePattern.of(recurrenceLength, recurrenceMask).bitwigMask();
        }

        public boolean hasAlternate() {
            return alternatePitch != null;
        }

        public Step withAlternate(final int newPitch, final int newVelocity, final double newGate, final double newChance,
                                  final boolean newAccent, final boolean newSlide, final int newRecurrenceLength,
                                  final int newRecurrenceMask) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, newPitch, Math.max(1, Math.min(127, newVelocity)),
                    Math.max(0.1, Math.min(1.25, newGate)), newChance, newAccent, newSlide,
                    newRecurrenceLength, newRecurrenceMask);
        }

        public Step withoutAlternate() {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, null, velocity, gate, chance, false, false, 0, 0);
        }

        public Step withAlternatePitch(final Integer newPitch) {
            if (newPitch == null) {
                return withoutAlternate();
            }
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, newPitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withAlternateVelocity(final int newVelocity) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, Math.max(1, Math.min(127, newVelocity)),
                    alternateGate, alternateChance, alternateAccent, alternateSlide,
                    alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withAlternateGate(final double newGate) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity,
                    Math.max(0.1, Math.min(1.25, newGate)), alternateChance, alternateAccent, alternateSlide,
                    alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withAlternateChance(final double newChance) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, newChance,
                    alternateAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withAlternateAccent(final boolean newAccent) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    newAccent, alternateSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withAlternateSlide(final boolean newSlide) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, newSlide, alternateRecurrenceLength, alternateRecurrenceMask);
        }

        public Step withAlternateRecurrence(final int length, final int mask) {
            return new Step(index, active, tieFromPrevious, pitch, velocity, gate, chance, accent, slide,
                    recurrenceLength, recurrenceMask, alternatePitch, alternateVelocity, alternateGate, alternateChance,
                    alternateAccent, alternateSlide, length, mask);
        }

        public int bitwigAlternateRecurrenceLength() {
            return RecurrencePattern.of(alternateRecurrenceLength, alternateRecurrenceMask).bitwigLength();
        }

        public int bitwigAlternateRecurrenceMask() {
            return RecurrencePattern.of(alternateRecurrenceLength, alternateRecurrenceMask).bitwigMask();
        }
    }

    private static int[] normalizeSameStepRecurrence(final int mainLength, final int mainMask,
                                                     final int alternateLength, final int alternateMask) {
        final RecurrencePattern main = RecurrencePattern.of(mainLength, mainMask);
        final RecurrencePattern alternate = RecurrencePattern.of(alternateLength, alternateMask);
        final int span = Math.max(main.effectiveSpan(), alternate.effectiveSpan());
        final int fullMask = (1 << span) - 1;
        int normalizedMain = main.effectiveMask(span);
        int normalizedAlternate = alternate.effectiveMask(span);

        if (normalizedMain == fullMask && normalizedAlternate != fullMask) {
            normalizedMain = fullMask & ~normalizedAlternate;
        } else if (normalizedAlternate == fullMask && normalizedMain != fullMask) {
            normalizedAlternate = fullMask & ~normalizedMain;
        } else {
            final int overlap = normalizedMain & normalizedAlternate;
            if (overlap != 0) {
                if (Integer.bitCount(normalizedMain) >= Integer.bitCount(normalizedAlternate)) {
                    normalizedMain &= ~overlap;
                } else {
                    normalizedAlternate &= ~overlap;
                }
            }
        }

        int available = (normalizedMain | normalizedAlternate) & fullMask;
        if (available == 0) {
            available = fullMask;
        }
        if (normalizedMain == 0 || normalizedAlternate == 0) {
            final int[] split = splitMask(available, span);
            normalizedMain = split[0];
            normalizedAlternate = split[1];
        }

        if ((normalizedMain & normalizedAlternate) != 0) {
            normalizedAlternate &= ~normalizedMain;
            if (normalizedAlternate == 0) {
                final int[] split = splitMask(normalizedMain | normalizedAlternate | available, span);
                normalizedMain = split[0];
                normalizedAlternate = split[1];
            }
        }

        final RecurrencePattern repairedMain = RecurrencePattern.of(span, normalizedMain);
        final RecurrencePattern repairedAlternate = RecurrencePattern.of(span, normalizedAlternate);
        return new int[]{repairedMain.length(), repairedMain.mask(), repairedAlternate.length(), repairedAlternate.mask()};
    }

    private static int[] splitMask(final int sourceMask, final int span) {
        int main = 0;
        int alternate = 0;
        boolean toMain = true;
        for (int bit = 0; bit < span; bit++) {
            if (((sourceMask >> bit) & 0x1) == 0) {
                continue;
            }
            if (toMain) {
                main |= 1 << bit;
            } else {
                alternate |= 1 << bit;
            }
            toMain = !toMain;
        }
        if (main == 0 && span > 0) {
            main = 1;
        }
        if (alternate == 0) {
            for (int bit = 0; bit < span; bit++) {
                if (((main >> bit) & 0x1) == 0) {
                    alternate |= 1 << bit;
                    break;
                }
            }
        }
        return new int[]{main, alternate};
    }
}
