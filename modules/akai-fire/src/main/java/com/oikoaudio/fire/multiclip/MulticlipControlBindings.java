package com.oikoaudio.fire.multiclip;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ButtonRowBindings;
import com.oikoaudio.fire.control.ContinuousEncoderScaler;
import com.oikoaudio.fire.control.PadMatrixBindings;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;

/** Owns the complete physical binding boundary for Multiclip Seq. */
final class MulticlipControlBindings {
    MulticlipControlBindings(
            final AkaiFireOikontrolExtension driver, final Layer layer, final Host host) {
        PadMatrixBindings.bindPressedVelocity(
                layer,
                driver.getRgbButtons(),
                new PadMatrixBindings.Host() {
                    @Override
                    public void handlePadPress(
                            final int padIndex, final boolean pressed, final int velocity) {
                        host.padPress(padIndex, pressed, velocity);
                    }

                    @Override
                    public RgbLightState padLight(final int padIndex) {
                        return host.padLight(padIndex);
                    }
                });

        bindGridButton(driver, layer, host, NoteAssign.BANK_L, -1);
        bindGridButton(driver, layer, host, NoteAssign.BANK_R, 1);
        driver.getButton(NoteAssign.ALT).bindPressed(layer, host::altButton, host::altLight);
        driver.getButton(NoteAssign.KNOB_MODE)
                .bindPressed(layer, host::knobModeButton, host::knobModeLight);

        final BiColorButton[] rowButtons = {
            driver.getButton(NoteAssign.MUTE_1),
            driver.getButton(NoteAssign.MUTE_2),
            driver.getButton(NoteAssign.MUTE_3),
            driver.getButton(NoteAssign.MUTE_4)
        };
        ButtonRowBindings.bindPressed(
                layer,
                rowButtons,
                new ButtonRowBindings.Host() {
                    @Override
                    public void handleButton(final int row, final boolean pressed) {
                        host.rowButton(row, pressed);
                    }

                    @Override
                    public BiColorLightState lightState(final int row) {
                        return host.rowLight(row);
                    }
                });

        for (int index = 0; index < driver.getEncoders().length; index++) {
            final int encoderIndex = index;
            driver.getEncoders()[index].bindContinuousEncoder(
                    layer,
                    driver::isGlobalShiftHeld,
                    ContinuousEncoderScaler.Profile.STRONG,
                    increment -> host.encoderTurn(encoderIndex, increment));
        }
    }

    private static void bindGridButton(
            final AkaiFireOikontrolExtension driver,
            final Layer layer,
            final Host host,
            final NoteAssign assignment,
            final int direction) {
        driver.getButton(assignment)
                .bindPressed(
                        layer,
                        pressed -> {
                            if (pressed) {
                                host.gridButton(direction);
                            }
                        },
                        () -> host.gridLight(direction));
    }

    interface Host {
        void padPress(int padIndex, boolean pressed, int velocity);

        RgbLightState padLight(int padIndex);

        void gridButton(int direction);

        BiColorLightState gridLight(int direction);

        void altButton(boolean pressed);

        BiColorLightState altLight();

        void rowButton(int row, boolean pressed);

        BiColorLightState rowLight(int row);

        void knobModeButton(boolean pressed);

        BiColorLightState knobModeLight();

        void encoderTurn(int encoderIndex, int increment);
    }
}
