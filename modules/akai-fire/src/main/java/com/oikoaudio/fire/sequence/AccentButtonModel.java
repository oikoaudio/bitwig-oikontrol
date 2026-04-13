package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Small model for the sequencer accent button.
 * Owns the latch state and derives velocity, light feedback, and OLED label text from it.
 */
final class AccentButtonModel {
    private static final int ACCENTED_VELOCITY = 127;

    private final AccentLatchState latchState = new AccentLatchState();

    AccentLatchState.Transition handlePressed(final boolean pressed) {
        return latchState.handlePressed(pressed);
    }

    void markModified() {
        if (latchState.isHeld()) {
            latchState.markModified();
        }
    }

    int currentVelocity(final int defaultVelocity) {
        return latchState.isActive() ? ACCENTED_VELOCITY : defaultVelocity;
    }

    int accentedVelocity() {
        return ACCENTED_VELOCITY;
    }

    BiColorLightState lightState() {
        return latchState.isActive() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
    }

    boolean isHolding() {
        return latchState.isHeld();
    }

    boolean isActive() {
        return latchState.isActive();
    }

    String label() {
        return latchState.isActive() ? "On" : "Off";
    }
}
