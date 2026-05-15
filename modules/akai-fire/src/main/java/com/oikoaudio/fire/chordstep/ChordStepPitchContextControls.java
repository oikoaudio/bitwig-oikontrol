package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Owns chord-step behavior for the physical pitch-context buttons.
 */
public final class ChordStepPitchContextControls {
    private final Host host;

    public ChordStepPitchContextControls(final Host host) {
        this.host = host;
    }

    public void handlePressed(final boolean pressed, final int amount, final boolean root) {
        if (!pressed) {
            return;
        }
        if (root) {
            host.adjustRoot(amount);
        } else {
            host.adjustOctave(amount);
        }
    }

    public BiColorLightState lightState(final int amount, final boolean root) {
        if (root) {
            return BiColorLightState.AMBER_HALF;
        }
        if (amount < 0) {
            return host.canLowerOctave() ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
        }
        return host.canRaiseOctave() ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
    }

    public interface Host {
        void adjustRoot(int amount);

        void adjustOctave(int amount);

        boolean canLowerOctave();

        boolean canRaiseOctave();
    }
}
