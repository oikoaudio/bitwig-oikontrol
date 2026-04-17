package com.oikoaudio.fire.control;

public final class EncoderTurnBehavior {
    private final ContinuousEncoderScaler scaler;
    private final EncoderStepAccumulator normalAccumulator;
    private final EncoderStepAccumulator fineAccumulator;

    private EncoderTurnBehavior(final ContinuousEncoderScaler scaler,
                                final EncoderStepAccumulator normalAccumulator,
                                final EncoderStepAccumulator fineAccumulator) {
        this.scaler = scaler;
        this.normalAccumulator = normalAccumulator;
        this.fineAccumulator = fineAccumulator;
    }

    public static EncoderTurnBehavior continuous() {
        return continuous(ContinuousEncoderScaler.Profile.STRONG);
    }

    public static EncoderTurnBehavior continuous(final ContinuousEncoderScaler.Profile profile) {
        return new EncoderTurnBehavior(new ContinuousEncoderScaler(profile), null, null);
    }

    public static EncoderTurnBehavior thresholded(final int normalThreshold, final int fineThreshold) {
        return new EncoderTurnBehavior(null,
                new EncoderStepAccumulator(normalThreshold),
                new EncoderStepAccumulator(fineThreshold));
    }

    public int apply(final int inc, final boolean fine) {
        if (scaler != null) {
            return scaler.scale(inc, fine);
        }
        final EncoderStepAccumulator accumulator = fine ? fineAccumulator : normalAccumulator;
        return accumulator.consume(inc);
    }

    public void reset() {
        if (scaler != null) {
            scaler.reset();
            return;
        }
        normalAccumulator.reset();
        fineAccumulator.reset();
    }
}
