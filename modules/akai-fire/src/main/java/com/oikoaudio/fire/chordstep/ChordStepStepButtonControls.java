package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Owns chord-step behavior for the physical STEP button.
 */
public final class ChordStepStepButtonControls {
    private final ChordStepAccentControls accentControls;
    private final Host host;

    public ChordStepStepButtonControls(final ChordStepAccentControls accentControls, final Host host) {
        this.accentControls = accentControls;
        this.host = host;
    }

    public void handlePressed(final boolean pressed) {
        if (pressed && host.hasHeldSteps()) {
            host.toggleAccentForHeldSteps();
            return;
        }
        if (host.isShiftHeld() || accentControls.isHeld()) {
            accentControls.handlePressed(pressed);
            return;
        }
        if (host.isAltHeld()) {
            if (pressed) {
                host.toggleFillMode();
                host.showValueInfo("Fill", host.isFillModeActive() ? "On" : "Off");
            }
            return;
        }
        if (!pressed) {
            return;
        }
        if (host.isStandaloneChordStepSurface()) {
            host.enterFugueStepMode();
        } else {
            host.enterMelodicStepMode();
        }
    }

    public BiColorLightState lightState(final boolean active) {
        if (!active) {
            return BiColorLightState.OFF;
        }
        if (host.isShiftHeld()) {
            return accentControls.isActive() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        if (host.isAltHeld()) {
            return host.stepFillLightState();
        }
        if (accentControls.isActive()) {
            return BiColorLightState.AMBER_FULL;
        }
        return host.isStandaloneChordStepSurface() ? host.modeButtonLightState() : BiColorLightState.OFF;
    }

    public interface Host {
        boolean hasHeldSteps();

        void toggleAccentForHeldSteps();

        boolean isShiftHeld();

        boolean isAltHeld();

        boolean isStandaloneChordStepSurface();

        void enterFugueStepMode();

        void enterMelodicStepMode();

        void toggleFillMode();

        boolean isFillModeActive();

        void showValueInfo(String title, String value);

        BiColorLightState stepFillLightState();

        BiColorLightState modeButtonLightState();
    }
}
