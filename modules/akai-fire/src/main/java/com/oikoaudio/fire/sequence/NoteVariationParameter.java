package com.oikoaudio.fire.sequence;

import java.util.Optional;

/** Continuous per-note values eligible for Baked Note Variation. */
public enum NoteVariationParameter {
    VELOCITY("Velocity", 0.0, 1.0, Double.POSITIVE_INFINITY),
    PRESSURE("Pressure", 0.0, 1.0, Double.POSITIVE_INFINITY),
    TIMBRE("Timbre", -1.0, 1.0, Double.POSITIVE_INFINITY),
    PITCH("Pitch", -96.0, 96.0, 12.0),
    PAN("Note Pan", -1.0, 1.0, Double.POSITIVE_INFINITY),
    GAIN("Note Gain", 0.0, 1.0, Double.POSITIVE_INFINITY),
    CHANCE("Chance", 0.0, 1.0, Double.POSITIVE_INFINITY),
    VELOCITY_SPREAD("Vel Spread", 0.0, 1.0, Double.POSITIVE_INFINITY);

    private final String displayName;
    private final double minimum;
    private final double maximum;
    private final double maximumDeviation;

    NoteVariationParameter(
            final String displayName,
            final double minimum,
            final double maximum,
            final double maximumDeviation) {
        this.displayName = displayName;
        this.minimum = minimum;
        this.maximum = maximum;
        this.maximumDeviation = maximumDeviation;
    }

    public String displayName() {
        return displayName;
    }

    public double clamp(final double value) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static Optional<NoteVariationParameter> from(final NoteStepAccess access) {
        return switch (access) {
            case VELOCITY -> Optional.of(VELOCITY);
            case PRESSURE -> Optional.of(PRESSURE);
            case TIMBRE -> Optional.of(TIMBRE);
            case PITCH -> Optional.of(PITCH);
            case CHANCE -> Optional.of(CHANCE);
            case VELOCITY_SPREAD -> Optional.of(VELOCITY_SPREAD);
            default -> Optional.empty();
        };
    }

    Bounds bounds(final double requestedDefault, final double requestedAmount) {
        final double center = clamp(requestedDefault);
        final double amount = Math.max(0.0, Math.min(1.0, requestedAmount));
        final double fullLower =
                Double.isFinite(maximumDeviation)
                        ? Math.max(minimum, center - maximumDeviation)
                        : minimum;
        final double fullUpper =
                Double.isFinite(maximumDeviation)
                        ? Math.min(maximum, center + maximumDeviation)
                        : maximum;
        return new Bounds(
                center + (fullLower - center) * amount,
                center,
                center + (fullUpper - center) * amount);
    }

    record Bounds(double lower, double center, double upper) {}
}
