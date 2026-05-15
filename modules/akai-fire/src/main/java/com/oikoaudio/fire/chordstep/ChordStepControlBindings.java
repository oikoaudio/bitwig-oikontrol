package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.RgbButton;
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
        final RgbButton[] pads = driver.getRgbButtons();
        for (int index = 0; index < pads.length; index++) {
            final int padIndex = index;
            pads[index].bindPressedVelocity(layer, velocity -> host.handlePadPress(padIndex, true, velocity),
                    () -> host.handlePadPress(padIndex, false, 0), () -> host.padLight(padIndex));
        }
    }

    private void bindButtons() {
        driver.getButton(NoteAssign.STEP_SEQ).bindPressed(layer, host::handleStepSeqPressed, host::stepSeqLightState);
        driver.getButton(NoteAssign.BANK_L).bindPressed(layer, pressed -> host.handleBankButton(pressed, -1),
                host::bankLightState);
        driver.getButton(NoteAssign.BANK_R).bindPressed(layer, pressed -> host.handleBankButton(pressed, 1),
                host::bankLightState);
        driver.getButton(NoteAssign.MUTE_1).bindPressed(layer, host::handleMute1Button, host::mute1LightState);
        driver.getButton(NoteAssign.MUTE_2).bindPressed(layer, host::handleMute2Button, host::mute2LightState);
        driver.getButton(NoteAssign.MUTE_3).bindPressed(layer, host::handleMute3Button, host::mute3LightState);
        driver.getButton(NoteAssign.MUTE_4).bindPressed(layer, host::handleMute4Button, host::mute4LightState);
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
