package com.oikoaudio.fire.sequence;

import java.util.EnumMap;
import java.util.Map;

public final class EncoderBankLayout {
    private final EnumMap<EncoderMode, EncoderBank> banks;

    public EncoderBankLayout(final Map<EncoderMode, EncoderBank> banks) {
        this.banks = new EnumMap<>(EncoderMode.class);
        this.banks.putAll(banks);
        for (final EncoderMode mode : EncoderMode.values()) {
            if (!this.banks.containsKey(mode)) {
                throw new IllegalArgumentException("Missing encoder bank for " + mode);
            }
        }
    }

    public EncoderBank bank(final EncoderMode mode) {
        return banks.get(mode);
    }
}
