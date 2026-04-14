package com.oikoaudio.fire.control;

/**
 * Scales rapid continuous encoder turns so slow turns stay precise while fast turns travel further.
 */
public final class ContinuousEncoderScaler {
    public enum Profile {
        STRONG(2, 4),
        GENTLE(2, 3),
        SOFT(1, 2);

        private final int mediumMultiplier;
        private final int fastMultiplier;

        Profile(final int mediumMultiplier, final int fastMultiplier) {
            this.mediumMultiplier = mediumMultiplier;
            this.fastMultiplier = fastMultiplier;
        }
    }

    private static final long FAST_WINDOW_NS = 45_000_000L;
    private static final long MEDIUM_WINDOW_NS = 100_000_000L;
    private static final long STREAK_RESET_NS = 220_000_000L;

    private final Profile profile;
    private long lastTurnNs = Long.MIN_VALUE;
    private int lastDirection = 0;
    private int streak = 0;

    public ContinuousEncoderScaler() {
        this(Profile.STRONG);
    }

    public ContinuousEncoderScaler(final Profile profile) {
        this.profile = profile;
    }

    public int scale(final int inc, final boolean fine) {
        if (inc == 0) {
            return 0;
        }
        if (fine) {
            reset();
            return inc;
        }

        final long now = System.nanoTime();
        final int direction = Integer.signum(inc);
        final long delta = lastTurnNs == Long.MIN_VALUE ? Long.MAX_VALUE : now - lastTurnNs;

        if (direction != lastDirection || delta > STREAK_RESET_NS) {
            streak = 1;
        } else {
            streak++;
        }

        lastTurnNs = now;
        lastDirection = direction;

        final int magnitude = Math.abs(inc);
        final int multiplier;
        if (delta <= FAST_WINDOW_NS && streak >= 3) {
            multiplier = profile.fastMultiplier;
        } else if (delta <= MEDIUM_WINDOW_NS && streak >= 2) {
            multiplier = profile.mediumMultiplier;
        } else {
            multiplier = 1;
        }
        return direction * magnitude * multiplier;
    }

    public void reset() {
        lastTurnNs = Long.MIN_VALUE;
        lastDirection = 0;
        streak = 0;
    }
}
