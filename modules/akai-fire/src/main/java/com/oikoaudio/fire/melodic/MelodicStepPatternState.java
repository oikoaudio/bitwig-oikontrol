package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class MelodicStepPatternState {
    private MelodicPattern currentPattern;
    private MelodicPattern basePattern;

    MelodicStepPatternState(final int defaultLoopSteps) {
        this.currentPattern = MelodicPattern.empty(defaultLoopSteps);
        this.basePattern = MelodicPattern.empty(defaultLoopSteps);
    }

    MelodicPattern currentPattern() {
        return currentPattern;
    }

    MelodicPattern basePattern() {
        return basePattern;
    }

    int loopSteps() {
        return currentPattern.loopSteps();
    }

    void setCurrentPattern(final MelodicPattern pattern) {
        currentPattern = pattern;
    }

    void setBasePattern(final MelodicPattern pattern) {
        basePattern = pattern;
    }

    void setCurrentAndBasePattern(final MelodicPattern pattern) {
        currentPattern = pattern;
        basePattern = pattern;
    }

    void applyObservedPattern(final MelodicPattern observed) {
        currentPattern = mergeObservedWithLatent(observed, currentPattern);
        basePattern = mergeObservedWithLatent(observed, basePattern);
    }

    MelodicPattern.Step ensureStep(final int stepIndex, final Supplier<MelodicPattern.Step> defaultStepSupplier) {
        final MelodicPattern.Step current = currentPattern.step(stepIndex);
        if (current.active()) {
            return current;
        }
        final MelodicPattern.Step created = defaultStepSupplier.get();
        currentPattern = currentPattern.withStep(created);
        return created;
    }

    MelodicPattern.Step restoreGeneratedStepOrDefault(final int stepIndex,
                                                      final Supplier<MelodicPattern.Step> defaultStepSupplier) {
        final MelodicPattern.Step base = basePattern.step(stepIndex);
        if (base.active() && base.pitch() != null) {
            return base.withIndex(stepIndex);
        }
        return defaultStepSupplier.get();
    }

    int activeStepCount(final MelodicPattern pattern) {
        int count = 0;
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (pattern.step(i).active()) {
                count++;
            }
        }
        return count;
    }

    private MelodicPattern mergeObservedWithLatent(final MelodicPattern observed,
                                                  final MelodicPattern latentSource) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            final MelodicPattern.Step observedStep = observed.step(i);
            if (observedStep.pitch() != null || latentSource == null) {
                steps.add(observedStep);
                continue;
            }
            final MelodicPattern.Step latentStep = latentSource.step(i);
            if (latentStep.pitch() != null) {
                steps.add(latentStep.withIndex(i).withActive(false));
            } else {
                steps.add(observedStep);
            }
        }
        return new MelodicPattern(steps, observed.loopSteps());
    }
}
