package com.oikoaudio.fire.control;

public final class EncoderStepAccumulator {
    private final int threshold;
    private int carry;

    public EncoderStepAccumulator(final int threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be positive");
        }
        this.threshold = threshold;
    }

    public int consume(final int inc) {
        if (inc == 0) {
            return 0;
        }
        carry += inc;
        final int steps = carry / threshold;
        carry %= threshold;
        return steps;
    }

    public void reset() {
        carry = 0;
    }
}
