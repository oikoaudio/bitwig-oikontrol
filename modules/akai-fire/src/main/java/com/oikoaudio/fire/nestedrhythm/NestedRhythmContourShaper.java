package com.oikoaudio.fire.nestedrhythm;

import com.oikoaudio.fire.sequence.RecurrencePattern;

final class NestedRhythmContourShaper {
    private NestedRhythmContourShaper() {
    }

    static double shapeUnitExpression(final int order,
                                      final double center,
                                      final double spread,
                                      final int rotation) {
        return clampUnit(center + spread * contourNormalized(order, rotation));
    }

    static double shapeSignedExpression(final int order,
                                        final double center,
                                        final double spread,
                                        final int rotation) {
        return clampSignedUnit(center + spread * contourNormalized(order, rotation));
    }

    static double shapePitchExpression(final int order,
                                       final double center,
                                       final double spread,
                                       final int rotation) {
        return clampPitchExpression(center + spread * contourNormalized(order, rotation));
    }

    static double shapeChance(final int order,
                              final NestedRhythmPattern.Role role,
                              final double baseline,
                              final double playChance,
                              final int rotation) {
        final double contour = (1.0 - contourNormalized(order, rotation)) * 0.5;
        final double attenuation = (1.0 - playChance) * chanceRoleWeight(role) * contour;
        return clampUnit(baseline - attenuation);
    }

    static RecurrencePattern generatedRecurrence(final int order,
                                                 final NestedRhythmPattern.Role role,
                                                 final double recurrenceDepth) {
        if (recurrenceDepth <= 0.0001) {
            return RecurrencePattern.of(0, 0);
        }
        final int span = RecurrencePattern.EDITOR_DEFAULT_SPAN;
        final double weakness = (1.0 - contourNormalized(order, 0)) * 0.5;
        final double dropoutStrength = recurrenceDepth * recurrenceRoleWeight(role) * (0.5 + weakness * 0.5);
        final int dropoutCount = Math.max(0, Math.min(span - 1,
                (int) Math.round(dropoutStrength * (span - 1))));
        if (dropoutCount == 0) {
            return RecurrencePattern.of(0, 0);
        }
        final int activeCount = span - dropoutCount;
        return RecurrencePattern.of(span, distributedMask(span, activeCount, order));
    }

    static int distributedMask(final int span, final int activeCount, final int rotation) {
        int mask = 0;
        for (int index = 0; index < span; index++) {
            final int previous = (index * activeCount) / span;
            final int next = ((index + 1) * activeCount) / span;
            if (next != previous) {
                mask |= 1 << Math.floorMod(index + rotation, span);
            }
        }
        if (mask == 0) {
            mask = 1 << Math.floorMod(rotation, span);
        }
        return mask;
    }

    static double contourNormalized(final int order, final int rotation) {
        return NestedRhythmGenerator.contourAt(order + rotation) / 18.0;
    }

    private static double chanceRoleWeight(final NestedRhythmPattern.Role role) {
        return switch (role) {
            case PRIMARY_ANCHOR -> 0.10;
            case SECONDARY_ANCHOR -> 0.25;
            case TUPLET_LEAD -> 0.22;
            case TUPLET_INTERIOR -> 0.75;
            case RATCHET_LEAD -> 0.18;
            case RATCHET_INTERIOR -> 0.82;
            case PICKUP -> 0.55;
        };
    }

    private static double recurrenceRoleWeight(final NestedRhythmPattern.Role role) {
        return switch (role) {
            case PRIMARY_ANCHOR -> 0.42;
            case SECONDARY_ANCHOR -> 0.50;
            case TUPLET_LEAD -> 0.54;
            case TUPLET_INTERIOR -> 0.82;
            case RATCHET_LEAD -> 0.58;
            case RATCHET_INTERIOR -> 0.92;
            case PICKUP -> 0.68;
        };
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
