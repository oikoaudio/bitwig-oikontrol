package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;

/**
 * Internal controller for chord-step sequencing behavior.
 */
public final class ChordStepController {
    private final ChordStepEditControls editControls;
    private final ChordStepClipController clipController;
    private final ChordStepObservationController observationController;

    public ChordStepController(final ChordStepEditControls editControls,
                               final ChordStepClipController clipController,
                               final ChordStepObservationController observationController) {
        this.editControls = editControls;
        this.clipController = clipController;
        this.observationController = observationController;
    }

    public void observeSelectedClip() {
        observationController.observeSelectedClip();
    }

    public void refreshSelectedClipState() {
        observationController.refreshSelectedClipState();
    }

    public void queueObservationResync() {
        observationController.queueResync();
    }

    public void refreshObservation() {
        observationController.refresh();
    }

    public void refreshObservationPass() {
        observationController.refreshPass();
    }

    public boolean ensureSelectedClip() {
        refreshSelectedClipState();
        return clipController.ensureSelectedClip();
    }

    public boolean ensureSelectedClipSlot() {
        refreshSelectedClipState();
        return clipController.ensureSelectedClipSlot();
    }

    public void handleMute1(final boolean pressed) {
        editControls.handleMute1(pressed);
    }

    public void handleMute2(final boolean pressed) {
        editControls.handleMute2(pressed);
    }

    public void handleMute3(final boolean pressed) {
        editControls.handleMute3(pressed);
    }

    public void handleMute4(final boolean pressed) {
        editControls.handleMute4(pressed);
    }

    public boolean isSelectHeld() {
        return editControls.isSelectHeld();
    }

    public boolean isFixedLengthHeld() {
        return editControls.isFixedLengthHeld();
    }

    public boolean isCopyHeld() {
        return editControls.isCopyHeld();
    }

    public boolean isDeleteHeld() {
        return editControls.isDeleteHeld();
    }

    public BiColorLightState mute1LightState() {
        return editControls.mute1LightState();
    }

    public BiColorLightState mute2LightState() {
        return editControls.mute2LightState();
    }

    public BiColorLightState mute3LightState() {
        return editControls.mute3LightState();
    }

    public BiColorLightState mute4LightState() {
        return editControls.mute4LightState();
    }

    public BooleanValueObject deleteHeldValue() {
        return editControls.deleteHeldValue();
    }

    public int slotIndex() {
        return clipController.slotIndex();
    }

    public RgbLigthState color() {
        return clipController.color();
    }
}
