package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;

/** Owns the stateless STEP, PATTERN, and pitch-context button policies for Chord Step. */
public final class ChordStepModeButtons {
    private final ChordStepAccentControls accentControls;
    private final Host host;

    public ChordStepModeButtons(final ChordStepAccentControls accentControls, final Host host) {
        this.accentControls = accentControls;
        this.host = host;
    }

    public void handleStepPressed(final boolean pressed) {
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
        if (pressed) {
            host.enterPlainStepPressTarget();
        }
    }

    public BiColorLightState stepLight(final boolean active) {
        if (!active) {
            return BiColorLightState.OFF;
        }
        if (host.isShiftHeld()) {
            return accentControls.isActive()
                    ? BiColorLightState.AMBER_FULL
                    : BiColorLightState.AMBER_HALF;
        }
        if (host.isAltHeld()) {
            return host.stepFillLightState();
        }
        if (accentControls.isActive()) {
            return BiColorLightState.AMBER_FULL;
        }
        return host.isStandaloneChordStepSurface()
                ? host.modeButtonLightState()
                : BiColorLightState.OFF;
    }

    public void handlePatternUpPressed(final boolean pressed) {
        handlePatternPressed(pressed, -1, true);
    }

    public void handlePatternDownPressed(final boolean pressed) {
        handlePatternPressed(pressed, 1, false);
    }

    private void handlePatternPressed(
            final boolean pressed, final int direction, final boolean enableBuilderLatch) {
        if (!pressed || host.isAltHeld()) {
            return;
        }
        if (host.isShiftHeld()) {
            host.setBuilderLatchEnabled(enableBuilderLatch);
            return;
        }
        if (host.canPageLeft() || host.canPageRight()) {
            host.page(direction);
        } else {
            host.showPageInfo();
        }
    }

    public BiColorLightState patternUpLight() {
        return host.canPageLeft() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
    }

    public BiColorLightState patternDownLight() {
        return host.canPageRight() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
    }

    public void handlePitchContextPressed(
            final boolean pressed, final int amount, final boolean root) {
        if (!pressed) {
            return;
        }
        if (root) {
            host.adjustRoot(amount);
        } else {
            host.adjustOctave(amount);
        }
    }

    public BiColorLightState pitchContextLight(final int amount, final boolean root) {
        if (root) {
            return BiColorLightState.AMBER_HALF;
        }
        final boolean available = amount < 0 ? host.canLowerOctave() : host.canRaiseOctave();
        return available ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
    }

    public interface Host {
        boolean hasHeldSteps();

        void toggleAccentForHeldSteps();

        boolean isShiftHeld();

        boolean isAltHeld();

        boolean isStandaloneChordStepSurface();

        void enterPlainStepPressTarget();

        void toggleFillMode();

        boolean isFillModeActive();

        void showValueInfo(String title, String value);

        BiColorLightState stepFillLightState();

        BiColorLightState modeButtonLightState();

        void setBuilderLatchEnabled(boolean enabled);

        void page(int direction);

        void showPageInfo();

        boolean canPageLeft();

        boolean canPageRight();

        void adjustRoot(int amount);

        void adjustOctave(int amount);

        boolean canLowerOctave();

        boolean canRaiseOctave();
    }
}
