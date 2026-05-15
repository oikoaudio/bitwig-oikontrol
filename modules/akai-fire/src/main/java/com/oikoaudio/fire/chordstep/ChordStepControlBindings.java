package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BankButtonBindings;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ButtonRowBindings;
import com.oikoaudio.fire.control.PadMatrixBindings;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;

final class ChordStepControlBindings {
    private final AkaiFireOikontrolExtension driver;
    private final Layer layer;
    private final Host host;

    ChordStepControlBindings(final AkaiFireOikontrolExtension driver, final Layer layer, final Host host) {
        this.driver = driver;
        this.layer = layer;
        this.host = host;
    }

    void bind() {
        bindPads();
        bindButtons();
    }

    void activatePatternButtons() {
        driver.getPatternButtons().setUpCallback(host::handlePatternUp, host::patternUpLight);
        driver.getPatternButtons().setDownCallback(host::handlePatternDown, host::patternDownLight);
    }

    void deactivatePatternButtons() {
        driver.getPatternButtons().setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        driver.getPatternButtons().setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
    }

    private void bindPads() {
        PadMatrixBindings.bindPressedVelocity(layer, driver.getRgbButtons(), new PadMatrixBindings.Host() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
                host.handlePadPress(padIndex, pressed, velocity);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return host.padLight(padIndex);
            }
        });
    }

    private void bindButtons() {
        driver.getButton(NoteAssign.STEP_SEQ).bindPressed(layer, host::handleStepSeqPressed, host::stepSeqLightState);
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
        final BiColorButton[] muteButtons = {
                driver.getButton(NoteAssign.MUTE_1),
                driver.getButton(NoteAssign.MUTE_2),
                driver.getButton(NoteAssign.MUTE_3),
                driver.getButton(NoteAssign.MUTE_4)
        };
        ButtonRowBindings.bindPressed(layer, muteButtons, new ButtonRowBindings.Host() {
            @Override
            public void handleButton(final int index, final boolean pressed) {
                handleMuteButton(index, pressed);
            }

            @Override
            public BiColorLightState lightState(final int index) {
                return muteLightState(index);
            }
        });
    }

    private void handleMuteButton(final int index, final boolean pressed) {
        switch (index) {
            case 0 -> host.handleMute1Button(pressed);
            case 1 -> host.handleMute2Button(pressed);
            case 2 -> host.handleMute3Button(pressed);
            case 3 -> host.handleMute4Button(pressed);
            default -> throw new IllegalArgumentException("Unsupported mute button index: " + index);
        }
    }

    private BiColorLightState muteLightState(final int index) {
        return switch (index) {
            case 0 -> host.mute1LightState();
            case 1 -> host.mute2LightState();
            case 2 -> host.mute3LightState();
            case 3 -> host.mute4LightState();
            default -> throw new IllegalArgumentException("Unsupported mute button index: " + index);
        };
    }

    interface Host {
        void handlePadPress(int padIndex, boolean pressed, int velocity);

        RgbLigthState padLight(int padIndex);

        void handleStepSeqPressed(boolean pressed);

        BiColorLightState stepSeqLightState();

        void handleBankButton(boolean pressed, int amount);

        BiColorLightState bankLightState();

        void handleMute1Button(boolean pressed);

        BiColorLightState mute1LightState();

        void handleMute2Button(boolean pressed);

        BiColorLightState mute2LightState();

        void handleMute3Button(boolean pressed);

        BiColorLightState mute3LightState();

        void handleMute4Button(boolean pressed);

        BiColorLightState mute4LightState();

        void handlePatternUp(boolean pressed);

        BiColorLightState patternUpLight();

        void handlePatternDown(boolean pressed);

        BiColorLightState patternDownLight();
    }
}
