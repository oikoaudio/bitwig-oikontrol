package com.oikoaudio.fire.nestedrhythm;

/** Owns immutable snapshots of Nested Rhythm's global structural and expression parameters. */
final class NestedRhythmParameterState {
    private Structural structural = Structural.defaults();
    private Expression expression = Expression.defaults();

    Structural structural() {
        return structural;
    }

    Expression expression() {
        return expression;
    }

    double density() {
        return structural.density();
    }

    double rate() {
        return structural.rate();
    }

    double cluster() {
        return structural.cluster();
    }

    int tupletDivisions() {
        return structural.tupletDivisions();
    }

    int tupletTargets() {
        return structural.tupletTargets();
    }

    int tupletTargetPhase() {
        return structural.tupletTargetPhase();
    }

    int ratchetDivisions() {
        return structural.ratchetDivisions();
    }

    int ratchetTargets() {
        return structural.ratchetTargets();
    }

    int ratchetTargetPhase() {
        return structural.ratchetTargetPhase();
    }

    NestedRhythmGenerator.RatchetTargetMode ratchetTargetMode() {
        return structural.ratchetTargetMode();
    }

    NestedRhythmGenerator.DensityDirection densityDirection() {
        return structural.densityDirection();
    }

    double velocityDepth() {
        return structural.velocityDepth();
    }

    int velocityCenter() {
        return structural.velocityCenter();
    }

    int velocityRotation() {
        return structural.velocityRotation();
    }

    int clipBarCount() {
        return structural.clipBarCount();
    }

    double recurrenceDepth() {
        return expression.recurrenceDepth();
    }

    double pressureCenter() {
        return expression.pressureCenter();
    }

    double pressureSpread() {
        return expression.pressureSpread();
    }

    int pressureRotation() {
        return expression.pressureRotation();
    }

    double timbreCenter() {
        return expression.timbreCenter();
    }

    double timbreSpread() {
        return expression.timbreSpread();
    }

    int timbreRotation() {
        return expression.timbreRotation();
    }

    double pitchExpressionCenter() {
        return expression.pitchExpressionCenter();
    }

    double pitchExpressionSpread() {
        return expression.pitchExpressionSpread();
    }

    int pitchExpressionRotation() {
        return expression.pitchExpressionRotation();
    }

    double chanceBaseline() {
        return expression.chanceBaseline();
    }

    double chancePlayProbability() {
        return expression.chancePlayProbability();
    }

    int chanceRotation() {
        return expression.chanceRotation();
    }

    void setDensity(double v) {
        structural = structural.withDensity(v);
    }

    void setRate(double v) {
        structural = structural.withRate(v);
    }

    void setCluster(double v) {
        structural = structural.withCluster(v);
    }

    void setTupletDivisions(int v) {
        structural = structural.withTupletDivisions(v);
    }

    void setTupletTargets(int v) {
        structural = structural.withTupletTargets(v);
    }

    void setTupletTargetPhase(int v) {
        structural = structural.withTupletTargetPhase(v);
    }

    void setRatchetDivisions(int v) {
        structural = structural.withRatchetDivisions(v);
    }

    void setRatchetTargets(int v) {
        structural = structural.withRatchetTargets(v);
    }

    void setRatchetTargetPhase(int v) {
        structural = structural.withRatchetTargetPhase(v);
    }

    void setRatchetTargetMode(NestedRhythmGenerator.RatchetTargetMode v) {
        structural = structural.withRatchetTargetMode(v);
    }

    void setDensityDirection(NestedRhythmGenerator.DensityDirection v) {
        structural = structural.withDensityDirection(v);
    }

    void setVelocityDepth(double v) {
        structural = structural.withVelocityDepth(v);
    }

    void setVelocityCenter(int v) {
        structural = structural.withVelocityCenter(v);
    }

    void setVelocityRotation(int v) {
        structural = structural.withVelocityRotation(v);
    }

    void setClipBarCount(int v) {
        structural = structural.withClipBarCount(v);
    }

    void setRecurrenceDepth(double v) {
        expression = expression.withRecurrenceDepth(v);
    }

    void setPressureCenter(double v) {
        expression = expression.withPressureCenter(v);
    }

    void setPressureSpread(double v) {
        expression = expression.withPressureSpread(v);
    }

    void setPressureRotation(int v) {
        expression = expression.withPressureRotation(v);
    }

    void setTimbreCenter(double v) {
        expression = expression.withTimbreCenter(v);
    }

    void setTimbreSpread(double v) {
        expression = expression.withTimbreSpread(v);
    }

    void setTimbreRotation(int v) {
        expression = expression.withTimbreRotation(v);
    }

    void setPitchExpressionCenter(double v) {
        expression = expression.withPitchExpressionCenter(v);
    }

    void setPitchExpressionSpread(double v) {
        expression = expression.withPitchExpressionSpread(v);
    }

    void setPitchExpressionRotation(int v) {
        expression = expression.withPitchExpressionRotation(v);
    }

    void setChanceBaseline(double v) {
        expression = expression.withChanceBaseline(v);
    }

    void setChancePlayProbability(double v) {
        expression = expression.withChancePlayProbability(v);
    }

    void setChanceRotation(int v) {
        expression = expression.withChanceRotation(v);
    }

    NestedRhythmGenerator.Settings generatorSettings(
            final int midiNote, final int meterNumerator, final int meterDenominator) {
        return new NestedRhythmGenerator.Settings(
                midiNote,
                density(),
                tupletDivisions(),
                tupletTargets(),
                tupletTargetPhase(),
                ratchetDivisions(),
                ratchetTargets(),
                ratchetTargetPhase(),
                velocityDepth(),
                velocityCenter(),
                velocityRotation(),
                0,
                cluster(),
                meterNumerator,
                meterDenominator,
                clipBarCount(),
                rate(),
                ratchetTargetMode(),
                densityDirection());
    }

    NestedRhythmExpressionSettings expressionSettings(final double clipStepSize) {
        return new NestedRhythmExpressionSettings(
                pressureCenter(),
                pressureSpread(),
                pressureRotation(),
                timbreCenter(),
                timbreSpread(),
                timbreRotation(),
                pitchExpressionCenter(),
                pitchExpressionSpread(),
                pitchExpressionRotation(),
                chanceBaseline(),
                chancePlayProbability(),
                chanceRotation(),
                recurrenceDepth(),
                clipStepSize);
    }

    record Structural(
            double density,
            double rate,
            double cluster,
            int tupletDivisions,
            int tupletTargets,
            int tupletTargetPhase,
            int ratchetDivisions,
            int ratchetTargets,
            int ratchetTargetPhase,
            NestedRhythmGenerator.RatchetTargetMode ratchetTargetMode,
            NestedRhythmGenerator.DensityDirection densityDirection,
            double velocityDepth,
            int velocityCenter,
            int velocityRotation,
            int clipBarCount) {
        static Structural defaults() {
            return new Structural(
                    1,
                    1,
                    0,
                    3,
                    0,
                    0,
                    2,
                    0,
                    0,
                    NestedRhythmGenerator.RatchetTargetMode.DEFAULT,
                    NestedRhythmGenerator.DensityDirection.KEEP_STRONG,
                    NestedRhythmGenerator.DEFAULT_VELOCITY_DEPTH,
                    100,
                    0,
                    1);
        }

        Structural withDensity(double v) {
            return new Structural(
                    v,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withRate(double v) {
            return new Structural(
                    density,
                    v,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withCluster(double v) {
            return new Structural(
                    density,
                    rate,
                    v,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withTupletDivisions(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    v,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withTupletTargets(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    v,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withTupletTargetPhase(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    v,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withRatchetDivisions(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    v,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withRatchetTargets(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    v,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withRatchetTargetPhase(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    v,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withRatchetTargetMode(NestedRhythmGenerator.RatchetTargetMode v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    v,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withDensityDirection(NestedRhythmGenerator.DensityDirection v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    v,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withVelocityDepth(double v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    v,
                    velocityCenter,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withVelocityCenter(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    v,
                    velocityRotation,
                    clipBarCount);
        }

        Structural withVelocityRotation(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    v,
                    clipBarCount);
        }

        Structural withClipBarCount(int v) {
            return new Structural(
                    density,
                    rate,
                    cluster,
                    tupletDivisions,
                    tupletTargets,
                    tupletTargetPhase,
                    ratchetDivisions,
                    ratchetTargets,
                    ratchetTargetPhase,
                    ratchetTargetMode,
                    densityDirection,
                    velocityDepth,
                    velocityCenter,
                    velocityRotation,
                    v);
        }
    }

    record Expression(
            double pressureCenter,
            double pressureSpread,
            int pressureRotation,
            double timbreCenter,
            double timbreSpread,
            int timbreRotation,
            double pitchExpressionCenter,
            double pitchExpressionSpread,
            int pitchExpressionRotation,
            double chanceBaseline,
            double chancePlayProbability,
            int chanceRotation,
            double recurrenceDepth) {
        static Expression defaults() {
            return new Expression(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0);
        }

        Expression withPressureCenter(double v) {
            return new Expression(
                    v,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withPressureSpread(double v) {
            return new Expression(
                    pressureCenter,
                    v,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withPressureRotation(int v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    v,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withTimbreCenter(double v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    v,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withTimbreSpread(double v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    v,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withTimbreRotation(int v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    v,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withPitchExpressionCenter(double v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    v,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withPitchExpressionSpread(double v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    v,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withPitchExpressionRotation(int v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    v,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withChanceBaseline(double v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    v,
                    chancePlayProbability,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withChancePlayProbability(double v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    v,
                    chanceRotation,
                    recurrenceDepth);
        }

        Expression withChanceRotation(int v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    v,
                    recurrenceDepth);
        }

        Expression withRecurrenceDepth(double v) {
            return new Expression(
                    pressureCenter,
                    pressureSpread,
                    pressureRotation,
                    timbreCenter,
                    timbreSpread,
                    timbreRotation,
                    pitchExpressionCenter,
                    pitchExpressionSpread,
                    pitchExpressionRotation,
                    chanceBaseline,
                    chancePlayProbability,
                    chanceRotation,
                    v);
        }
    }
}
