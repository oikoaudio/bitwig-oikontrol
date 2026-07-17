package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.control.BankButtonBindings;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ButtonRowBindings;
import com.oikoaudio.fire.control.PadMatrixBindings;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TrackSelectIndicatorLights;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.utils.PatternButtons;

final class ChordStepControlBindings {
    private final Layer layer;
    private final RgbButton[] pads;
    private final BiColorButton stepButton;
    private final BiColorButton bankLeftButton;
    private final BiColorButton bankRightButton;
    private final BiColorButton[] rowButtons;
    private final PatternButtons patternButtons;
    private final MultiStateHardwareLight[] stateLights;
    private final Host host;

    ChordStepControlBindings(
            final Layer layer,
            final RgbButton[] pads,
            final BiColorButton stepButton,
            final BiColorButton bankLeftButton,
            final BiColorButton bankRightButton,
            final BiColorButton[] rowButtons,
            final PatternButtons patternButtons,
            final MultiStateHardwareLight[] stateLights,
            final Host host) {
        this.layer = layer;
        this.pads = pads;
        this.stepButton = stepButton;
        this.bankLeftButton = bankLeftButton;
        this.bankRightButton = bankRightButton;
        this.rowButtons = rowButtons;
        this.patternButtons = patternButtons;
        this.stateLights = stateLights;
        this.host = host;
    }

    void bind() {
        bindPads();
        bindButtons();
        bindEditStatusLights();
    }

    void activatePatternButtons() {
        patternButtons.setUpCallback(host::handlePatternUp, host::patternUpLight);
        patternButtons.setDownCallback(host::handlePatternDown, host::patternDownLight);
    }

    void deactivatePatternButtons() {
        patternButtons.setUpCallback(pressed -> {}, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> {}, () -> BiColorLightState.OFF);
    }

    private void bindPads() {
        PadMatrixBindings.bindPressedVelocity(
                layer,
                pads,
                new PadMatrixBindings.Host() {
                    @Override
                    public void handlePadPress(
                            final int padIndex, final boolean pressed, final int velocity) {
                        host.handlePadPress(padIndex, pressed, velocity);
                    }

                    @Override
                    public RgbLightState padLight(final int padIndex) {
                        return host.padLight(padIndex);
                    }
                });
    }

    private void bindButtons() {
        stepButton.bindPressed(layer, host::handleStepSeqPressed, host::stepSeqLightState);
        BankButtonBindings.bind(
                layer,
                bankLeftButton,
                bankRightButton,
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
        ButtonRowBindings.bindPressed(
                layer,
                rowButtons,
                new ButtonRowBindings.Host() {
                    @Override
                    public void handleButton(final int index, final boolean pressed) {
                        handleRowButton(index, pressed);
                    }

                    @Override
                    public BiColorLightState lightState(final int index) {
                        return rowLightState(index);
                    }
                });
    }

    private void handleRowButton(final int index, final boolean pressed) {
        switch (index) {
            case 0 -> host.handleMute1Button(pressed);
            case 1 -> host.handleMute2Button(pressed);
            case 2 -> host.handleMute3Button(pressed);
            case 3 -> host.handleMute4Button(pressed);
            default ->
                    throw new IllegalArgumentException("Unsupported mute button index: " + index);
        }
    }

    private BiColorLightState rowLightState(final int index) {
        return switch (index) {
            case 0 -> host.mute1LightState();
            case 1 -> host.mute2LightState();
            case 2 -> host.mute3LightState();
            case 3 -> host.mute4LightState();
            default ->
                    throw new IllegalArgumentException("Unsupported mute button index: " + index);
        };
    }

    private void bindEditStatusLights() {
        for (int index = 0; index < stateLights.length; index++) {
            final int lightIndex = index;
            layer.bindLightState(() -> editStatusLightState(lightIndex), stateLights[lightIndex]);
        }
    }

    private BiColorLightState editStatusLightState(final int index) {
        return switch (index) {
            case 0 ->
                    TrackSelectIndicatorLights.green(
                            BiColorLightState.GREEN_FULL.equals(host.mute1LightState()));
            case 1 ->
                    TrackSelectIndicatorLights.green(
                            BiColorLightState.GREEN_FULL.equals(host.mute2LightState()));
            case 2 ->
                    TrackSelectIndicatorLights.green(
                            BiColorLightState.GREEN_FULL.equals(host.mute3LightState()));
            case 3 ->
                    TrackSelectIndicatorLights.red(
                            BiColorLightState.GREEN_FULL.equals(host.mute4LightState()));
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported edit status light index: " + index);
        };
    }

    /*
     * The host remains semantic: physical controls are supplied separately by the composition root.
     */
    interface Host {
        void handlePadPress(int padIndex, boolean pressed, int velocity);

        RgbLightState padLight(int padIndex);

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
