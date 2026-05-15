package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.PadBankRowControlBindings;
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
        PadBankRowControlBindings.velocitySensitivePads(driver, layer, padBankRowHost(),
                new PadBankRowControlBindings.ExtraButtonBinding(NoteAssign.STEP_SEQ,
                        host::handleStepSeqPressed, host::stepSeqLightState)).bind();
    }

    void activatePatternButtons() {
        driver.getPatternButtons().setUpCallback(host::handlePatternUp, host::patternUpLight);
        driver.getPatternButtons().setDownCallback(host::handlePatternDown, host::patternDownLight);
    }

    void deactivatePatternButtons() {
        driver.getPatternButtons().setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        driver.getPatternButtons().setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
    }

    private PadBankRowControlBindings.Host padBankRowHost() {
        return new PadBankRowControlBindings.Host() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed) {
                host.handlePadPress(padIndex, pressed, 0);
            }

            @Override
            public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
                host.handlePadPress(padIndex, pressed, velocity);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return host.padLight(padIndex);
            }

            @Override
            public void handleBankButton(final boolean pressed, final int amount) {
                host.handleBankButton(pressed, amount);
            }

            @Override
            public BiColorLightState bankLightState() {
                return host.bankLightState();
            }

            @Override
            public void handleRowButton(final int index, final boolean pressed) {
                switch (index) {
                    case 0 -> host.handleMute1Button(pressed);
                    case 1 -> host.handleMute2Button(pressed);
                    case 2 -> host.handleMute3Button(pressed);
                    case 3 -> host.handleMute4Button(pressed);
                    default -> throw new IllegalArgumentException("Unsupported mute button index: " + index);
                }
            }

            @Override
            public BiColorLightState rowLightState(final int index) {
                return switch (index) {
                    case 0 -> host.mute1LightState();
                    case 1 -> host.mute2LightState();
                    case 2 -> host.mute3LightState();
                    case 3 -> host.mute4LightState();
                    default -> throw new IllegalArgumentException("Unsupported mute button index: " + index);
                };
            }
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
