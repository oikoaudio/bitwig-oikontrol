package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.List;

final class NestedRhythmEditablePattern {
    private final List<NestedRhythmEditablePulse> pulses = new ArrayList<>();

    List<NestedRhythmEditablePulse> pulses() {
        return pulses;
    }

    void applyGeneratedPattern(final NestedRhythmPattern pattern,
                               final NestedRhythmExpressionSettings settings,
                               final int totalFineStepCount) {
        final List<NestedRhythmEditablePulse> previousPulses = List.copyOf(pulses);
        pulses.clear();
        for (final NestedRhythmPattern.PulseEvent event : pattern.events()) {
            final NestedRhythmEditablePulse pulse = new NestedRhythmEditablePulse(event);
            pulse.applyGeneratedRecurrence(settings);
            pulses.add(restoreLocalEdits(pulse, previousPulses, totalFineStepCount));
        }
    }

    void resetEdits() {
        for (final NestedRhythmEditablePulse pulse : pulses) {
            pulse.resetEdits();
        }
    }

    void refreshGeneratedRecurrenceDefaults(final NestedRhythmExpressionSettings settings) {
        for (final NestedRhythmEditablePulse pulse : pulses) {
            if (!pulse.recurrenceEdited) {
                pulse.applyGeneratedRecurrence(settings);
            }
        }
    }

    int findSelectedPulseIndex(final int previousSelectionFineStart, final int previousSelectedPulseIndex) {
        if (pulses.isEmpty()) {
            return -1;
        }
        if (previousSelectionFineStart < 0) {
            return Math.min(Math.max(previousSelectedPulseIndex, 0), pulses.size() - 1);
        }
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < pulses.size(); index++) {
            final int distance = Math.abs(pulses.get(index).fineStart - previousSelectionFineStart);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private NestedRhythmEditablePulse restoreLocalEdits(final NestedRhythmEditablePulse current,
                                                       final List<NestedRhythmEditablePulse> previousPulses,
                                                       final int totalFineStepCount) {
        final NestedRhythmEditablePulse match = findOverlayMatch(current, previousPulses, totalFineStepCount);
        if (match != null) {
            current.copyLocalEditsFrom(match);
        }
        return current;
    }

    private NestedRhythmEditablePulse findOverlayMatch(final NestedRhythmEditablePulse current,
                                                      final List<NestedRhythmEditablePulse> previousPulses,
                                                      final int totalFineStepCount) {
        NestedRhythmEditablePulse best = null;
        int bestScore = Integer.MAX_VALUE;
        for (final NestedRhythmEditablePulse previous : previousPulses) {
            final int score = matchScore(current, previous);
            if (score < bestScore) {
                bestScore = score;
                best = previous;
            }
        }
        return bestScore <= overlayMatchTolerance(totalFineStepCount) ? best : null;
    }

    private int matchScore(final NestedRhythmEditablePulse current, final NestedRhythmEditablePulse previous) {
        if (current.midiNote != previous.midiNote || current.role != previous.role) {
            return Integer.MAX_VALUE;
        }
        final int fineStartDelta = Math.abs(current.fineStart - previous.fineStart);
        final int orderDelta = Math.abs(current.order - previous.order);
        return fineStartDelta + orderDelta * 16;
    }

    private int overlayMatchTolerance(final int totalFineStepCount) {
        return Math.max(24, totalFineStepCount / 64);
    }
}
