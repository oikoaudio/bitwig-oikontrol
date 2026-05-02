package com.oikoaudio.fire.nestedrhythm;

import java.util.Arrays;

final class NestedRhythmLoopLength {
    static final int STEP_COUNT = 32;
    private static final double EPSILON = 0.0001;

    private NestedRhythmLoopLength() {
    }

    static int normalizeLastStepIndex(final int stepIndex) {
        return Math.max(0, Math.min(STEP_COUNT - 1, stepIndex));
    }

    static int loopFineSteps(final int totalFineSteps, final int lastStepIndex) {
        final int clampedTotalFineSteps = Math.max(1, totalFineSteps);
        final int normalizedLastStepIndex = normalizeLastStepIndex(lastStepIndex);
        return Math.max(1, (int) Math.round(
                clampedTotalFineSteps * ((normalizedLastStepIndex + 1) / (double) STEP_COUNT)));
    }

    static double loopLengthBeats(final double totalBeats, final int lastStepIndex) {
        final double clampedTotalBeats = Math.max(0.25, totalBeats);
        return clampedTotalBeats * (normalizeLastStepIndex(lastStepIndex) + 1) / STEP_COUNT;
    }

    static Settings settingsFromBeats(final double loopLengthBeats,
                                      final double beatsPerBar,
                                      final int[] supportedBarCounts) {
        final double normalizedBeatsPerBar = Math.max(0.25, beatsPerBar);
        final double normalizedLength = Math.max(0.25, loopLengthBeats);
        int selectedBarCount = supportedBarCounts.length == 0 ? 1 : Math.max(1, supportedBarCounts[0]);
        for (final int candidate : supportedBarCounts) {
            final int normalizedCandidate = Math.max(1, candidate);
            selectedBarCount = normalizedCandidate;
            if (normalizedLength <= normalizedCandidate * normalizedBeatsPerBar + 0.0001) {
                break;
            }
        }
        final double containingLength = selectedBarCount * normalizedBeatsPerBar;
        final int lastStepIndex = normalizeLastStepIndex((int) Math.round(
                normalizedLength / containingLength * STEP_COUNT) - 1);
        return new Settings(selectedBarCount, lastStepIndex);
    }

    static double steppedBarLengthBeats(final double currentBeats,
                                        final double beatsPerBar,
                                        final int[] supportedBarCounts,
                                        final int amount) {
        final int[] counts = normalizedBarCounts(supportedBarCounts);
        if (amount == 0) {
            return Math.max(0.25, currentBeats);
        }
        double result = Math.max(0.25, currentBeats);
        final int direction = amount > 0 ? 1 : -1;
        for (int step = 0; step < Math.abs(amount); step++) {
            result = nextSupportedBarLengthBeats(result, Math.max(0.25, beatsPerBar), counts, direction);
        }
        return result;
    }

    static double relativeLengthBeats(final double currentBeats,
                                      final int direction,
                                      final double minBeats,
                                      final double maxBeats) {
        final double minimum = Math.max(0.25, minBeats);
        final double maximum = Math.max(minimum, maxBeats);
        if (direction < 0) {
            return Math.max(minimum, currentBeats / 2.0);
        }
        if (direction > 0) {
            if (currentBeats >= maximum - EPSILON) {
                return currentBeats;
            }
            return Math.min(maximum, Math.max(minimum, currentBeats) * 2.0);
        }
        return currentBeats;
    }

    private static double nextSupportedBarLengthBeats(final double currentBeats,
                                                      final double beatsPerBar,
                                                      final int[] counts,
                                                      final int direction) {
        if (direction > 0) {
            for (final int count : counts) {
                final double candidate = count * beatsPerBar;
                if (candidate > currentBeats + EPSILON) {
                    return candidate;
                }
            }
            return currentBeats;
        }
        for (int index = counts.length - 1; index >= 0; index--) {
            final double candidate = counts[index] * beatsPerBar;
            if (candidate < currentBeats - EPSILON) {
                return candidate;
            }
        }
        return currentBeats;
    }

    private static int[] normalizedBarCounts(final int[] supportedBarCounts) {
        final int[] counts = Arrays.stream(supportedBarCounts)
                .map(count -> Math.max(1, count))
                .distinct()
                .sorted()
                .toArray();
        return counts.length == 0 ? new int[]{1} : counts;
    }

    record Settings(int barCount, int lastStepIndex) {
    }
}
