package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class IndispensabilityRanker {
    private IndispensabilityRanker() {
    }

    static Map<Integer, RankedPulse> rank(final List<Integer> starts, final int totalFineSteps) {
        if (starts.isEmpty() || totalFineSteps <= 0) {
            return Map.of();
        }
        final List<Integer> orderedStarts = starts.stream().distinct().sorted().toList();
        final Map<Integer, Double> rawScores = new HashMap<>();
        for (int index = 0; index < orderedStarts.size(); index++) {
            final int start = orderedStarts.get(index);
            final int nextStart = orderedStarts.get((index + 1) % orderedStarts.size());
            final double coincidence = metricWeight(start, totalFineSteps);
            final double anticipation = metricWeight(nextStart, totalFineSteps) * 0.25;
            final double downbeat = start == 0 ? 0.50 : 0.0;
            rawScores.put(start, coincidence + anticipation + downbeat);
        }

        final double min = rawScores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        final double max = rawScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        final double range = Math.max(0.0001, max - min);
        final List<Integer> strongestFirst = new ArrayList<>(orderedStarts);
        strongestFirst.sort(Comparator
                .comparingDouble((Integer start) -> rawScores.get(start))
                .reversed()
                .thenComparingInt(Integer::intValue));

        final Map<Integer, RankedPulse> ranked = new HashMap<>();
        for (int order = 0; order < strongestFirst.size(); order++) {
            final int start = strongestFirst.get(order);
            final double raw = rawScores.get(start);
            ranked.put(start, new RankedPulse(start, raw, (raw - min) / range, order));
        }
        return ranked;
    }

    private static double metricWeight(final int fineStart, final int totalFineSteps) {
        if (fineStart <= 0) {
            return 1.0;
        }
        final int common = greatestCommonDivisor(fineStart, Math.max(1, totalFineSteps));
        return Math.log(common + 1.0) / Math.log(Math.max(2, totalFineSteps) + 1.0);
    }

    private static int greatestCommonDivisor(final int left, final int right) {
        int a = Math.abs(left);
        int b = Math.abs(right);
        while (b != 0) {
            final int next = a % b;
            a = b;
            b = next;
        }
        return Math.max(1, a);
    }
}
