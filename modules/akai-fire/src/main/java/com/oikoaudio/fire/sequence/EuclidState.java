package com.oikoaudio.fire.sequence;

class EuclidState {
    private int length = 16;
    private int pulses = 4;
    private int rotation = 0;
    private int accentPulses = 0;
    private boolean inverted = false;

    int getLength() {
        return length;
    }

    int getPulses() {
        return pulses;
    }

    int getRotation() {
        return rotation;
    }

    int getAccentPulses() {
        return accentPulses;
    }

    boolean isInverted() {
        return inverted;
    }

    void incLength(int delta) {
        length = Math.max(1, Math.min(16, length + delta));
        pulses = Math.min(pulses, length);
        rotation = rotation % length;
    }

    void incPulses(int delta) {
        pulses = Math.max(0, Math.min(length, pulses + delta));
        accentPulses = Math.min(accentPulses, pulses);
    }

    void incRotation(int delta) {
        if (length <= 0) {
            rotation = 0;
            return;
        }
        rotation = ((rotation + delta) % length + length) % length;
    }

    void incAccentPulses(int delta) {
        accentPulses = Math.max(0, Math.min(pulses, accentPulses + delta));
    }

    void toggleInvert() {
        inverted = !inverted;
    }
}
