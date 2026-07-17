package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.rhythm.MetricIndispensabilityRanker;
import com.oikoaudio.fire.rhythm.MetricIndispensabilityRanker.RankedPosition;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** Chooses musical thin/fill targets while leaving note mutation to Melodic Step. */
final class MelodicDensityEditor {
    private MelodicDensityEditor() {}

    static OptionalInt weakestRemovableStep(final MelodicPattern pattern) {
        final MelodicPatternAnalyzer.Analysis analysis = MelodicPatternAnalyzer.analyze(pattern);
        if (analysis.activeSteps().isEmpty()) {
            return OptionalInt.empty();
        }
        final Map<Integer, RankedPosition> ranked =
                MetricIndispensabilityRanker.rank(analysis.activeSteps(), pattern.loopSteps());
        return analysis.activeSteps().stream()
                .filter(step -> !analysis.anchorSteps().contains(step))
                .max(
                        Comparator.comparingInt((Integer step) -> ranked.get(step).rankOrder())
                                .thenComparingInt(Integer::intValue))
                .map(OptionalInt::of)
                .orElseGet(OptionalInt::empty);
    }

    static OptionalInt strongestRestorableStep(
            final MelodicPattern current, final MelodicPattern base) {
        final List<Integer> baseSteps = MelodicPatternAnalyzer.analyze(base).activeSteps();
        final Map<Integer, RankedPosition> ranked =
                MetricIndispensabilityRanker.rank(baseSteps, base.loopSteps());
        return baseSteps.stream()
                .filter(step -> !current.step(step).active())
                .min(
                        Comparator.comparingInt((Integer step) -> ranked.get(step).rankOrder())
                                .thenComparingInt(Integer::intValue))
                .map(OptionalInt::of)
                .orElseGet(OptionalInt::empty);
    }

    static OptionalLong regenerationSeed(
            final long lastGenerationSeed, final long currentSeed, final int activeStepCount) {
        if (activeStepCount <= 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(lastGenerationSeed >= 0 ? lastGenerationSeed : currentSeed);
    }

    static String parameterLabel(final double density) {
        return "%.2f".formatted(Math.max(0.0, Math.min(1.0, density)));
    }
}
