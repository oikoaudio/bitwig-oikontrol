package com.oikoaudio.fire.nestedrhythm;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BankButtonBindings;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ButtonRowBindings;
import com.oikoaudio.fire.control.PadMatrixBindings;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;

final class NestedRhythmControlBindings {
    private final AkaiFireOikontrolExtension driver;
    private final Layer layer;
    private final Host host;

    NestedRhythmControlBindings(final AkaiFireOikontrolExtension driver, final Layer layer, final Host host) {
        this.driver = driver;
        this.layer = layer;
        this.host = host;
    }

    void bind() {
        bindPads();
        bindButtons();
    }

    private void bindPads() {
        PadMatrixBindings.bindPressed(layer, driver.getRgbButtons(), new PadMatrixBindings.PressHost() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed) {
                host.handlePadPress(padIndex, pressed);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return host.padLight(padIndex);
            }
        });
    }

    private void bindButtons() {
        final BiColorButton[] muteButtons = {
                driver.getButton(NoteAssign.MUTE_1),
                driver.getButton(NoteAssign.MUTE_2),
                driver.getButton(NoteAssign.MUTE_3),
                driver.getButton(NoteAssign.MUTE_4)
        };
        ButtonRowBindings.bindPressed(layer, muteButtons, new ButtonRowBindings.Host() {
            @Override
            public void handleButton(final int index, final boolean pressed) {
                host.handleMuteButton(index, pressed);
            }

            @Override
            public BiColorLightState lightState(final int index) {
                return host.muteLightState(index);
            }
        });

        BankButtonBindings.bind(layer, driver.getButton(NoteAssign.BANK_L), driver.getButton(NoteAssign.BANK_R),
                new BankButtonBindings.Host() {
                    @Override
                    public void handleBankButton(final boolean pressed, final int amount) {
                        host.handleBankButton(pressed, amount);
                    }

                    @Override
                    public BiColorLightState bankLightState() {
                        return host.bankLightState();
                    }
                });
    }

    interface Host {
        void handlePadPress(int padIndex, boolean pressed);

        RgbLigthState padLight(int padIndex);

        void handleBankButton(boolean pressed, int amount);

        BiColorLightState bankLightState();

        void handleMuteButton(int index, boolean pressed);

        BiColorLightState muteLightState(int index);
    }
}
