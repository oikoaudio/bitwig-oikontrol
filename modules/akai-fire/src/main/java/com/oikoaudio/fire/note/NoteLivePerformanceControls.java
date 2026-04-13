package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.BiColorLightState;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Live-note performance controls owned by Note mode's mute buttons outside chord-step editing.
 */
final class NoteLivePerformanceControls {
    private final Consumer<Integer> sustainSender;
    private final Consumer<Integer> sostenutoSender;
    private final Runnable noteRepeatToggle;
    private final BooleanSupplier noteRepeatActive;
    private final StatusDisplay statusDisplay;
    private boolean sustainActive;
    private boolean sostenutoActive;

    NoteLivePerformanceControls(final Consumer<Integer> sustainSender,
                                final Consumer<Integer> sostenutoSender,
                                final Runnable noteRepeatToggle,
                                final BooleanSupplier noteRepeatActive,
                                final StatusDisplay statusDisplay) {
        this.sustainSender = sustainSender;
        this.sostenutoSender = sostenutoSender;
        this.noteRepeatToggle = noteRepeatToggle;
        this.noteRepeatActive = noteRepeatActive;
        this.statusDisplay = statusDisplay;
    }

    void handleMute1(final boolean pressed) {
        if (!pressed) {
            return;
        }
        sustainActive = !sustainActive;
        sustainSender.accept(sustainActive ? 127 : 0);
        statusDisplay.show("Sustain", sustainActive ? "On" : "Off");
    }

    void handleMute2(final boolean pressed) {
        if (!pressed) {
            return;
        }
        sostenutoActive = !sostenutoActive;
        sostenutoSender.accept(sostenutoActive ? 127 : 0);
        statusDisplay.show("Sostenuto", sostenutoActive ? "On" : "Off");
    }

    void handleMute3(final boolean pressed) {
        if (pressed) {
            noteRepeatToggle.run();
        }
    }

    BiColorLightState mute1LightState() {
        return sustainActive ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    BiColorLightState mute2LightState() {
        return sostenutoActive ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
    }

    BiColorLightState mute3LightState() {
        return noteRepeatActive.getAsBoolean() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    void resetToggles() {
        if (sustainActive) {
            sustainActive = false;
            sustainSender.accept(0);
        }
        if (sostenutoActive) {
            sostenutoActive = false;
            sostenutoSender.accept(0);
        }
    }

    @FunctionalInterface
    interface StatusDisplay {
        void show(String title, String detail);
    }
}
