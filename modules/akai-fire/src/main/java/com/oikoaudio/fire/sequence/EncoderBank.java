package com.oikoaudio.fire.sequence;

public record EncoderBank(String modeInfo, EncoderSlotBinding[] slots) {
    public EncoderBank {
        if (slots.length != 4) {
            throw new IllegalArgumentException("EncoderBank requires exactly 4 slots");
        }
    }
}
