package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;

public enum EncoderMode {
    CHANNEL(BiColorLightState.MODE_CHANNEL),
    MIXER(BiColorLightState.MODE_MIXER),
	USER_1(BiColorLightState.MODE_USER1),
	USER_2(BiColorLightState.MODE_USER2);

    private final BiColorLightState state;

    EncoderMode(final BiColorLightState state) {
        this.state = state;
    }

    public BiColorLightState getState() {
        return state;
    }

}
