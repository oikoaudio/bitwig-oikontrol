package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ButtonRowBindings;
import com.oikoaudio.fire.control.PadMatrixBindings;
import com.oikoaudio.fire.control.TrackSelectIndicatorLights;
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
        bindEditStatusLights(driver.getStateLights(), layer, host);
    }

    private static void bindEditStatusLights(
            final MultiStateHardwareLight[] stateLights, final Layer layer, final Host host) {
        for (int row = 0; row < stateLights.length; row++) {
            final int lightRow = row;
            layer.bindLightState(
                    () ->
                            editStatusLightState(
                                    lightRow,
                                    BiColorLightState.GREEN_FULL.equals(host.rowLight(lightRow))),
                    stateLights[lightRow]);
        }
    }

    static BiColorLightState editStatusLightState(final int row, final boolean active) {
        return switch (row) {
            case 0, 1, 2 -> TrackSelectIndicatorLights.green(active);
            case 3 -> TrackSelectIndicatorLights.red(active);
            default -> throw new IllegalArgumentException("Unsupported mute button row: " + row);
        };
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
    }
}
