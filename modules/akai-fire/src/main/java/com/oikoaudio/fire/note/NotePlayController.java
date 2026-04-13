package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Internal controller for live note-play behavior.
 */
final class NotePlayController {
    private final NoteLiveControlSurface controls;
    private final NoteLivePadPerformer padPerformer;

    NotePlayController(final NoteLiveControlSurface controls, final NoteLivePadPerformer padPerformer) {
        this.controls = controls;
        this.padPerformer = padPerformer;
    }

    void activate() {
        controls.activate();
    }

    void deactivate(final Runnable releaseHeldNotes) {
        releaseHeldNotes.run();
        controls.deactivate();
    }

    void activateEncoders() {
        controls.activateEncoders();
    }

    void deactivateEncoders() {
        controls.deactivateEncoders();
    }

    void handlePadPress(final int padIndex, final boolean pressed, final int velocity, final int liveVelocity) {
        padPerformer.handlePadPress(padIndex, pressed, velocity, liveVelocity);
    }

    void handleMute1(final boolean pressed) {
        controls.handleMute1(pressed);
    }

    void handleMute2(final boolean pressed) {
        controls.handleMute2(pressed);
    }

    void handleMute3(final boolean pressed) {
        controls.handleMute3(pressed);
    }

    BiColorLightState mute1LightState() {
        return controls.mute1LightState();
    }

    BiColorLightState mute2LightState() {
        return controls.mute2LightState();
    }

    BiColorLightState mute3LightState() {
        return controls.mute3LightState();
    }

    void handleModeAdvance(final boolean pressed, final boolean noteStepActive) {
        controls.handleModeAdvance(pressed, noteStepActive);
    }

    BiColorLightState modeLightState() {
        return controls.modeLightState();
    }
}
