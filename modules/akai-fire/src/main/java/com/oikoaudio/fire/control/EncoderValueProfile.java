package com.oikoaudio.fire.control;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableRangedValue;

/**
 * Shared adjustment profiles for parameter-like encoder targets.
 */
public enum EncoderValueProfile {
    LARGE_RANGE(0.01, 0.0025),
    COMPACT_RANGE(0.005, 0.00125),
    SEMITONE_PARAMETER(0.0416667, 0.0416667),
    PITCH_PARAMETER(0.0138889, 0.0138889);

    private final double coarseIncrement;
    private final double fineIncrement;

    EncoderValueProfile(final double coarseIncrement, final double fineIncrement) {
        this.coarseIncrement = coarseIncrement;
        this.fineIncrement = fineIncrement;
    }

    public double increment(final boolean fine) {
        return fine ? fineIncrement : coarseIncrement;
    }

    public double delta(final boolean fine, final int inc) {
        return inc * increment(fine);
    }

    public void adjustValue(final SettableRangedValue value, final boolean fine, final int inc) {
        final double nextValue = Math.max(0.0, Math.min(1.0, value.get() + delta(fine, inc)));
        value.setImmediately(nextValue);
    }

    public void adjustParameter(final Parameter parameter, final boolean fine, final int inc) {
        adjustValue(parameter.value(), fine, inc);
    }
}
