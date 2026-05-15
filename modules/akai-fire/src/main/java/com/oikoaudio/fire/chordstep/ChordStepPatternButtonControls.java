package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Owns chord-step behavior for the physical PATTERN up/down buttons.
 */
public final class ChordStepPatternButtonControls {
    private final Host host;

    public ChordStepPatternButtonControls(final Host host) {
        this.host = host;
    }

    public void handleUpPressed(final boolean pressed) {
        if (pressed && !host.isAltHeld()) {
            host.page(-1);
        }
    }

    public void handleDownPressed(final boolean pressed) {
        if (pressed && !host.isAltHeld()) {
            host.page(1);
        }
    }

    public BiColorLightState upLight() {
        return host.canPageLeft() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
    }

    public BiColorLightState downLight() {
        return host.canPageRight() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
    }

    public interface Host {
        boolean isAltHeld();

        void page(int direction);

        boolean canPageLeft();

        boolean canPageRight();
    }
}
