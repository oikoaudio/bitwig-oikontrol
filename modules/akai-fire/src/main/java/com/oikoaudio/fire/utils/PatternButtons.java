package com.oikoaudio.fire.utils;

import com.oikoaudio.fire.AkaiFireDrumSeqExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PatternButtons {
    private final BiColorButton upButton;
    private final BiColorButton downButton;

    // We'll store the callbacks here:
    private Consumer<Boolean> upCallback = pressed -> {};
    private Consumer<Boolean> downCallback = pressed -> {};
    private Supplier<BiColorLightState> upLightSupplier = () -> BiColorLightState.OFF;
    private Supplier<BiColorLightState> downLightSupplier = () -> BiColorLightState.OFF;

    public PatternButtons(AkaiFireDrumSeqExtension driver, Layer layer) {
        upButton = driver.getButton(NoteAssign.PATTERN_UP);
        downButton = driver.getButton(NoteAssign.PATTERN_DOWN);

        upButton.markPressedInteressed();
        downButton.markPressedInteressed();

        // Bind the buttons to delegate to our stored callbacks:
        upButton.bindPressed(layer, pressed -> {
            if (pressed) {
                upCallback.accept(true);
            }
        }, upLightSupplier);

        downButton.bindPressed(layer, pressed -> {
            if (pressed) {
                downCallback.accept(true);
            }
        }, downLightSupplier);
    }

    // Methods to register callbacks:
    public void setUpCallback(Consumer<Boolean> callback, Supplier<BiColorLightState> lightSupplier) {
        this.upCallback = callback;
        this.upLightSupplier = lightSupplier;
    }

    public void setDownCallback(Consumer<Boolean> callback, Supplier<BiColorLightState> lightSupplier) {
        this.downCallback = callback;
        this.downLightSupplier = lightSupplier;
    }

    public BiColorButton getUpButton() {
        return upButton;
    }

    public BiColorButton getDownButton() {
        return downButton;
    }

}
