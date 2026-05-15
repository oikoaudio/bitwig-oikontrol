package com.oikoaudio.fire.nestedrhythm;

import com.oikoaudio.fire.sequence.RecurrencePattern;

final class NestedRhythmEditablePulse {
    final int order;
    final int fineStart;
    final int baseDuration;
    int midiNote;
    final int baseVelocity;
    final NestedRhythmPattern.Role role;
    final double indispensability;
    int velocityOffset = 0;
    double gateScale = 1.0;
    double pressureOffset = 0.0;
    double timbreOffset = 0.0;
    double pitchExpressionOffset = 0.0;
    double chanceOffset = 0.0;
    int recurrenceLength = 0;
    int recurrenceMask = 0;
    boolean recurrenceEdited = false;
    boolean enabled = true;

    NestedRhythmEditablePulse(final NestedRhythmPattern.PulseEvent event) {
        this.order = event.order();
        this.fineStart = event.fineStart();
        this.baseDuration = event.duration();
        this.midiNote = event.midiNote();
        this.baseVelocity = event.velocity();
        this.role = event.role();
        this.indispensability = event.indispensability();
    }

    int effectiveVelocity() {
        return clamp(1, 127, baseVelocity + velocityOffset);
    }

    double effectivePressure(final NestedRhythmExpressionSettings settings) {
        return clampUnit(NestedRhythmContourShaper.shapeUnitExpression(
                order, settings.pressureCenter(), settings.pressureSpread(), settings.pressureRotation())
                + pressureOffset);
    }

    double effectiveTimbre(final NestedRhythmExpressionSettings settings) {
        return clampSignedUnit(NestedRhythmContourShaper.shapeSignedExpression(
                order, settings.timbreCenter(), settings.timbreSpread(), settings.timbreRotation())
                + timbreOffset);
    }

    double effectivePitchExpression(final NestedRhythmExpressionSettings settings) {
        return clampPitchExpression(NestedRhythmContourShaper.shapePitchExpression(
                order,
                settings.pitchExpressionCenter(),
                settings.pitchExpressionSpread(),
                settings.pitchExpressionRotation())
                + pitchExpressionOffset);
    }

    double effectiveChance(final NestedRhythmExpressionSettings settings) {
        return clampUnit(NestedRhythmContourShaper.shapeChance(
                order,
                role,
                indispensability,
                settings.chanceBaseline(),
                settings.chancePlayProbability(),
                settings.chanceRotation())
                + chanceOffset);
    }

    RecurrencePattern effectiveRecurrence() {
        return RecurrencePattern.of(recurrenceLength, recurrenceMask);
    }

    void applyGeneratedRecurrence(final NestedRhythmExpressionSettings settings) {
        final RecurrencePattern recurrence = NestedRhythmContourShaper.generatedRecurrence(
                order, role, indispensability, settings.recurrenceDepth());
        recurrenceLength = recurrence.length();
        recurrenceMask = recurrence.mask();
    }

    double effectiveDuration() {
        return Math.max(1.0, baseDuration * gateScale);
    }

    double effectiveBeatDuration(final NestedRhythmExpressionSettings settings) {
        return effectiveDuration() * settings.clipStepSize();
    }

    boolean containsFineStep(final int fineStep, final int clipFineSteps) {
        final int duration = Math.max(1, (int) Math.round(effectiveDuration()));
        final int end = Math.floorMod(fineStart + duration, clipFineSteps);
        if (duration >= clipFineSteps) {
            return true;
        }
        if (fineStart < end) {
            return fineStep >= fineStart && fineStep < end;
        }
        return fineStep >= fineStart || fineStep < end;
    }

    void resetEdits() {
        velocityOffset = 0;
        gateScale = 1.0;
        pressureOffset = 0.0;
        timbreOffset = 0.0;
        pitchExpressionOffset = 0.0;
        chanceOffset = 0.0;
        recurrenceLength = 0;
        recurrenceMask = 0;
        recurrenceEdited = false;
        enabled = true;
    }

    void copyLocalEditsFrom(final NestedRhythmEditablePulse other) {
        velocityOffset = other.velocityOffset;
        gateScale = other.gateScale;
        pressureOffset = other.pressureOffset;
        timbreOffset = other.timbreOffset;
        pitchExpressionOffset = other.pitchExpressionOffset;
        chanceOffset = other.chanceOffset;
        recurrenceLength = other.recurrenceLength;
        recurrenceMask = other.recurrenceMask;
        recurrenceEdited = other.recurrenceEdited;
        enabled = other.enabled;
    }

    String roleLabel() {
        return switch (role) {
            case PRIMARY_ANCHOR -> "Anchor";
            case SECONDARY_ANCHOR -> "Support";
            case TUPLET_LEAD -> "Tuplet Lead";
            case TUPLET_INTERIOR -> "Tuplet";
            case RATCHET_LEAD -> "Ratchet Lead";
            case RATCHET_INTERIOR -> "Ratchet";
            case PICKUP -> "Pickup";
        };
    }

    private static int clamp(final int min, final int max, final int value) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampUnit(final double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double clampSignedUnit(final double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    private static double clampPitchExpression(final double value) {
        return Math.max(-24.0, Math.min(24.0, value));
    }
}
