package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.List;

public final class MelodicPatternAnalyzer {
    private MelodicPatternAnalyzer() {
    }

    public static Analysis analyze(final MelodicPattern pattern) {
        final List<Integer> anchors = new ArrayList<>();
        final List<Integer> activeSteps = new ArrayList<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (!step.active()) {
                continue;
            }
            activeSteps.add(i);
            if (i == 0 || i % 4 == 0 || i == pattern.loopSteps() - 1) {
                anchors.add(i);
            }
        }
        if (anchors.isEmpty() && !activeSteps.isEmpty()) {
            anchors.add(activeSteps.get(0));
        }
        return new Analysis(activeSteps, anchors);
    }

    public record Analysis(List<Integer> activeSteps, List<Integer> anchorSteps) {
    }
}
