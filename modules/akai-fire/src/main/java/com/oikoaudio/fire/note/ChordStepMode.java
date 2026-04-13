package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;

/**
 * Internal mode object for chord-step sequencing behavior.
 */
final class ChordStepMode {
    private final NoteChordStepEditControls editControls;
    private final NoteChordStepClipController clipController;
    private final NoteChordStepObservationController observationController;

    ChordStepMode(final NoteChordStepEditControls editControls,
                  final NoteChordStepClipController clipController,
                  final NoteChordStepObservationController observationController) {
        this.editControls = editControls;
        this.clipController = clipController;
        this.observationController = observationController;
    }

    void observeSelectedClip() {
        observationController.observeSelectedClip();
    }

    void refreshSelectedClipState() {
        observationController.refreshSelectedClipState();
    }

    void queueObservationResync() {
        observationController.queueResync();
    }

    void refreshObservation() {
        observationController.refresh();
    }

    void refreshObservationPass() {
        observationController.refreshPass();
    }

    boolean ensureSelectedClip() {
        refreshSelectedClipState();
        return clipController.ensureSelectedClip();
    }

    boolean ensureSelectedClipSlot() {
        refreshSelectedClipState();
        return clipController.ensureSelectedClipSlot();
    }

    void handleMute1(final boolean pressed) {
        editControls.handleMute1(pressed);
    }

    void handleMute2(final boolean pressed) {
        editControls.handleMute2(pressed);
    }

    void handleMute3(final boolean pressed) {
        editControls.handleMute3(pressed);
    }

    void handleMute4(final boolean pressed) {
        editControls.handleMute4(pressed);
    }

    boolean isSelectHeld() {
        return editControls.isSelectHeld();
    }

    boolean isFixedLengthHeld() {
        return editControls.isFixedLengthHeld();
    }

    boolean isCopyHeld() {
        return editControls.isCopyHeld();
    }

    boolean isDeleteHeld() {
        return editControls.isDeleteHeld();
    }

    BiColorLightState mute1LightState() {
        return editControls.mute1LightState();
    }

    BiColorLightState mute2LightState() {
        return editControls.mute2LightState();
    }

    BiColorLightState mute3LightState() {
        return editControls.mute3LightState();
    }

    BiColorLightState mute4LightState() {
        return editControls.mute4LightState();
    }

    BooleanValueObject deleteHeldValue() {
        return editControls.deleteHeldValue();
    }

    int slotIndex() {
        return clipController.slotIndex();
    }

    RgbLigthState color() {
        return clipController.color();
    }
}
