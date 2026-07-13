package com.oikoaudio.fire.nestedrhythm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NestedRhythmParameterStateTest {
    @Test
    void defaultsProduceTheBaselineGeneratorAndExpressionSettings() {
        final NestedRhythmParameterState state = new NestedRhythmParameterState();

        assertEquals(
                new NestedRhythmGenerator.Settings(
                        60,
                        1.0,
                        3,
                        0,
                        0,
                        2,
                        0,
                        0,
                        NestedRhythmGenerator.DEFAULT_VELOCITY_DEPTH,
                        100,
                        0,
                        0,
                        0.0,
                        7,
                        8,
                        1,
                        1.0,
                        NestedRhythmGenerator.RatchetTargetMode.DEFAULT,
                        NestedRhythmGenerator.DensityDirection.KEEP_STRONG),
                state.generatorSettings(60, 7, 8));
        assertEquals(
                new NestedRhythmExpressionSettings(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0.125),
                state.expressionSettings(0.125));
    }

    @Test
    void structuralAndExpressionSnapshotsRemainIndependent() {
        final NestedRhythmParameterState state = new NestedRhythmParameterState();
        final NestedRhythmParameterState.Structural initialStructural = state.structural();

        state.setDensity(0.5);
        state.setTupletTargets(2);
        state.setPressureSpread(0.4);
        state.setChanceBaseline(0.75);

        assertEquals(1.0, initialStructural.density());
        assertEquals(0.5, state.structural().density());
        assertEquals(2, state.structural().tupletTargets());
        assertEquals(0.4, state.expression().pressureSpread());
        assertEquals(0.75, state.expression().chanceBaseline());
    }
}
