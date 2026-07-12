package com.oikoaudio.fire.fugue;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.PadBankRowControlBindings;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;

/** Owns Fugue's pad-bank, bank-button, line-button, and encoder-mode hardware mapping. */
public final class FugueControlBindings implements PadBankRowControlBindings.Host {
    private final Port port;

    public FugueControlBindings(final AkaiFireOikontrolExtension driver, final Layer owner, final Port port) {
        this.port = port;
        new PadBankRowControlBindings(driver, owner, this,
                new PadBankRowControlBindings.ExtraButtonBinding(NoteAssign.KNOB_MODE,
                        port::encoderModeButton, port::encoderModeLight)).bind();
    }

    @Override
    public void handlePadPress(final int padIndex, final boolean pressed) {
        port.padPress(padIndex, pressed);
    }

    @Override
    public RgbLigthState padLight(final int padIndex) {
        return port.padLight(padIndex);
    }

    @Override
    public void handleBankButton(final boolean pressed, final int amount) {
        port.bankButton(pressed, amount);
    }

    @Override
    public BiColorLightState bankLightState() {
        return BiColorLightState.HALF;
    }

    @Override
    public void handleRowButton(final int index, final boolean pressed) {
        port.lineButton(index, pressed);
    }

    @Override
    public BiColorLightState rowLightState(final int index) {
        return port.lineLight(index);
    }

    public interface Port {
        void padPress(int padIndex, boolean pressed);

        RgbLigthState padLight(int padIndex);

        void bankButton(boolean pressed, int amount);

        void lineButton(int index, boolean pressed);

        BiColorLightState lineLight(int index);

        void encoderModeButton(boolean pressed);

        BiColorLightState encoderModeLight();
    }
}
