package com.oikoaudio.fire.control;

/**
 * Converts Bitwig relative adjustment amounts into signed logical encoder units.
 */
public final class RelativeEncoderMagnitude {
    private static final double MAX_RELATIVE_SPEED = 63.0;
    private static final int STANDARD_FAST_TURN_LIMIT = 8;

    private RelativeEncoderMagnitude() {
    }

    public static int toSignedUnits(final double adjustment) {
        if (adjustment == 0.0) {
            return 0;
        }
        final double scaled = adjustment * MAX_RELATIVE_SPEED;
        return (int) (scaled > 0 ? Math.ceil(scaled) : Math.floor(scaled));
    }

    public static int toDirectionStep(final int signedUnits) {
        return Integer.signum(signedUnits);
    }

    public static int toBoundedMagnitudeStep(final int signedUnits, final int maxMagnitude) {
        if (maxMagnitude < 1) {
            throw new IllegalArgumentException("maxMagnitude must be positive");
        }
        final int direction = Integer.signum(signedUnits);
        final int magnitude = Math.min(Math.abs(signedUnits), maxMagnitude);
        return direction * magnitude;
    }

    public static int toStandardTurnStep(final int signedUnits) {
        return toStandardTurnStep(signedUnits, false);
    }

    public static int toStandardTurnStep(final int signedUnits, final boolean fine) {
        if (fine) {
            return toDirectionStep(signedUnits);
        }
        final int direction = Integer.signum(signedUnits);
        final int observedMagnitude = Math.min(Math.abs(signedUnits), STANDARD_FAST_TURN_LIMIT);
        final int logicalMagnitude;
        if (observedMagnitude <= 3) {
            logicalMagnitude = observedMagnitude == 0 ? 0 : 1;
        } else if (observedMagnitude <= 6) {
            logicalMagnitude = 4;
        } else {
            logicalMagnitude = STANDARD_FAST_TURN_LIMIT;
        }
        return direction * logicalMagnitude;
    }
}
