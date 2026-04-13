package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Owns the chord-step edit button state used on the mute buttons while chord-step mode is active.
 */
final class NoteChordStepEditControls {
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final ValueDisplay valueDisplay;
    private final Runnable clearDisplay;

    NoteChordStepEditControls(final ValueDisplay valueDisplay, final Runnable clearDisplay) {
        this.valueDisplay = valueDisplay;
        this.clearDisplay = clearDisplay;
    }

    void handleMute1(final boolean pressed) {
        selectHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Select", "Load step");
        } else {
            clearDisplay.run();
        }
    }

    void handleMute2(final boolean pressed) {
        fixedLengthHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Last Step", "Target step");
        } else {
            clearDisplay.run();
        }
    }

    void handleMute3(final boolean pressed) {
        copyHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Paste", "Clip / step target");
        } else {
            clearDisplay.run();
        }
    }

    void handleMute4(final boolean pressed) {
        deleteHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Delete", "Clip / step target");
        } else {
            clearDisplay.run();
        }
    }

    boolean isSelectHeld() {
        return selectHeld.get();
    }

    boolean isFixedLengthHeld() {
        return fixedLengthHeld.get();
    }

    boolean isCopyHeld() {
        return copyHeld.get();
    }

    boolean isDeleteHeld() {
        return deleteHeld.get();
    }

    BiColorLightState mute1LightState() {
        return selectHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    BiColorLightState mute2LightState() {
        return fixedLengthHeld.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
    }

    BiColorLightState mute3LightState() {
        return copyHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
    }

    BiColorLightState mute4LightState() {
        return deleteHeld.get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
    }

    BooleanValueObject deleteHeldValue() {
        return deleteHeld;
    }

    @FunctionalInterface
    interface ValueDisplay {
        void show(String title, String detail);
    }
}
