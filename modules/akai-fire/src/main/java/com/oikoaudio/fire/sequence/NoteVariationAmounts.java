package com.oikoaudio.fire.sequence;

import java.util.EnumMap;
import java.util.Map;

/** Session-local Baked Note Variation amount for each eligible parameter. */
public final class NoteVariationAmounts {
    private final Map<NoteVariationParameter, Double> amounts =
            new EnumMap<>(NoteVariationParameter.class);

    public double amount(final NoteVariationParameter parameter) {
        return amounts.getOrDefault(parameter, 0.0);
    }

    public double adjust(final NoteVariationParameter parameter, final double delta) {
        return set(parameter, amount(parameter) + delta);
    }

    public double set(final NoteVariationParameter parameter, final double value) {
        final double clamped = Math.max(0.0, Math.min(1.0, value));
        amounts.put(parameter, clamped);
        return clamped;
    }
}
