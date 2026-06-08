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
        if (!pressed || host.isAltHeld()) {
            return;
        }
        if (host.isShiftHeld()) {
            host.setBuilderLatchEnabled(true);
            return;
        }
        if (host.canPageLeft() || host.canPageRight()) {
            host.page(-1);
        } else {
            host.showPageInfo();
        }
    }

    public void handleDownPressed(final boolean pressed) {
        if (!pressed || host.isAltHeld()) {
            return;
        }
        if (host.isShiftHeld()) {
            host.setBuilderLatchEnabled(false);
            return;
        }
        if (host.canPageLeft() || host.canPageRight()) {
            host.page(1);
        } else {
            host.showPageInfo();
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

        boolean isShiftHeld();

        void setBuilderLatchEnabled(boolean enabled);

        void page(int direction);

        void showPageInfo();

        boolean canPageLeft();

        boolean canPageRight();
    }
}
