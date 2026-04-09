package com.oikoaudio.fire.control;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableRangedValue;

public final class MixerEncoderProfile {
    public static final double STEP_SIZE = 0.25;
    public static final double COARSE_INCREMENT = 0.01;
    public static final double FINE_INCREMENT = 0.0025;

    private MixerEncoderProfile() {
    }

    public static double increment(final boolean fine) {
        return fine ? FINE_INCREMENT : COARSE_INCREMENT;
    }

    public static void adjustValue(final SettableRangedValue value, final boolean fine, final int inc) {
        final double nextValue = Math.max(0.0, Math.min(1.0, value.get() + (inc * increment(fine))));
        value.setImmediately(nextValue);
    }

    public static void adjustParameter(final Parameter parameter, final boolean fine, final int inc) {
        adjustValue(parameter.value(), fine, inc);
    }
}
