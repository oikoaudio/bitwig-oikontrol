package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.display.EncoderFooterLegend;

public record EncoderBank(String modeInfo, String footerLegend, EncoderSlotBinding[] slots) {
    public EncoderBank(final String modeInfo, final EncoderSlotBinding[] slots) {
        this(modeInfo, EncoderFooterLegend.fromModeInfo(modeInfo), slots);
    }

    public EncoderBank {
        if (slots.length != 4) {
            throw new IllegalArgumentException("EncoderBank requires exactly 4 slots");
        }
    }
}
