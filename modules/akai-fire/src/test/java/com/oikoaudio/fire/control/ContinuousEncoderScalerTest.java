package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContinuousEncoderScalerTest {
    @Test
    void strongProfileScalesSameDirectionMediumAndFastStreaks() {
        final TestClock clock = new TestClock();
        final ContinuousEncoderScaler scaler = new ContinuousEncoderScaler(ContinuousEncoderScaler.Profile.STRONG,
                clock::now);

        assertEquals(1, scaler.scale(1, false));
        clock.advanceNanos(80_000_000L);
        assertEquals(2, scaler.scale(1, false));
        clock.advanceNanos(40_000_000L);
        assertEquals(4, scaler.scale(1, false));
    }

    @Test
    void fineModeDisablesAccelerationAndResetsStreak() {
        final TestClock clock = new TestClock();
        final ContinuousEncoderScaler scaler = new ContinuousEncoderScaler(ContinuousEncoderScaler.Profile.STRONG,
                clock::now);

        assertEquals(1, scaler.scale(1, false));
        clock.advanceNanos(40_000_000L);
        assertEquals(1, scaler.scale(1, true));
        clock.advanceNanos(40_000_000L);
        assertEquals(1, scaler.scale(1, false));
    }

    @Test
    void directionChangeResetsAccelerationStreak() {
        final TestClock clock = new TestClock();
        final ContinuousEncoderScaler scaler = new ContinuousEncoderScaler(ContinuousEncoderScaler.Profile.STRONG,
                clock::now);

        assertEquals(1, scaler.scale(1, false));
        clock.advanceNanos(40_000_000L);
        assertEquals(-1, scaler.scale(-1, false));
    }

    private static final class TestClock {
        private long now;

        private long now() {
            return now;
        }

        private void advanceNanos(final long nanos) {
            now += nanos;
        }
    }
}

