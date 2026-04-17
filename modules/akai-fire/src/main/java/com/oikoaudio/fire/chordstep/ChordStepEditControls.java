package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Owns the chord-step edit button state used on the mute buttons while chord-step mode is active.
 */
public final class ChordStepEditControls {
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final ValueDisplay valueDisplay;
    private final Runnable clearDisplay;

    public ChordStepEditControls(final ValueDisplay valueDisplay, final Runnable clearDisplay) {
        this.valueDisplay = valueDisplay;
        this.clearDisplay = clearDisplay;
    }

    public void handleMute1(final boolean pressed) {
        selectHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Select", "Load step");
        } else {
            clearDisplay.run();
        }
    }

    public void handleMute2(final boolean pressed) {
        fixedLengthHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Last Step", "Target step");
        } else {
            clearDisplay.run();
        }
    }

    public void handleMute3(final boolean pressed) {
        copyHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Paste", "Clip / step target");
        } else {
            clearDisplay.run();
        }
    }

    public void handleMute4(final boolean pressed) {
        deleteHeld.set(pressed);
        if (pressed) {
            valueDisplay.show("Delete", "Clip / step target");
        } else {
            clearDisplay.run();
        }
    }

    public boolean isSelectHeld() {
        return selectHeld.get();
    }

    public boolean isFixedLengthHeld() {
        return fixedLengthHeld.get();
    }

    public boolean isCopyHeld() {
        return copyHeld.get();
    }

    public boolean isDeleteHeld() {
        return deleteHeld.get();
    }

    public BiColorLightState mute1LightState() {
        return selectHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    public BiColorLightState mute2LightState() {
        return fixedLengthHeld.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
    }

    public BiColorLightState mute3LightState() {
        return copyHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
    }

    public BiColorLightState mute4LightState() {
        return deleteHeld.get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
    }

    public BooleanValueObject deleteHeldValue() {
        return deleteHeld;
    }

    @FunctionalInterface
    public interface ValueDisplay {
        void show(String title, String detail);
    }
}
