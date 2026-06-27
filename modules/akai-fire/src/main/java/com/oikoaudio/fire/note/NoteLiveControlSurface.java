package com.oikoaudio.fire.note;

import com.oikoaudio.fire.control.EncoderTouchDisplayHandler;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.sequence.EncoderMode;

/**
 * Facade for Note mode's live-side button and encoder-page control surface.
 */
final class NoteLiveControlSurface {
    private final NoteLivePerformanceControls performanceControls;
    private final NoteLiveEncoderModeControls encoderModeControls;
    private final EncoderTouchDisplayHandler touchDisplayHandler;
    private final ValueDisplay valueDisplay;
    private final DetailDisplay detailDisplay;
    private final Runnable clearDisplay;
    private final ParameterEncoderBinding.ExplicitResetControl explicitResetControl;

    NoteLiveControlSurface(final NoteLivePerformanceControls performanceControls,
                           final NoteLiveEncoderModeControls encoderModeControls,
                           final EncoderTouchDisplayHandler touchDisplayHandler,
                           final ValueDisplay valueDisplay,
                           final DetailDisplay detailDisplay,
                           final Runnable clearDisplay,
                           final ParameterEncoderBinding.ExplicitResetControl explicitResetControl) {
        this.performanceControls = performanceControls;
        this.encoderModeControls = encoderModeControls;
        this.touchDisplayHandler = touchDisplayHandler;
        this.valueDisplay = valueDisplay;
        this.detailDisplay = detailDisplay;
        this.clearDisplay = clearDisplay;
        this.explicitResetControl = explicitResetControl;
    }

    NoteLiveControlSurface(final NoteLivePerformanceControls performanceControls,
                           final NoteLiveEncoderModeControls encoderModeControls,
                           final EncoderTouchDisplayHandler touchDisplayHandler,
                           final ValueDisplay valueDisplay,
                           final DetailDisplay detailDisplay,
                           final Runnable clearDisplay) {
        this(performanceControls, encoderModeControls, touchDisplayHandler, valueDisplay, detailDisplay, clearDisplay,
                ParameterEncoderBinding.ExplicitResetControl.none());
    }

    void activate() {
        encoderModeControls.resetToChannel();
    }

    void deactivate() {
        performanceControls.resetToggles();
        encoderModeControls.deactivateAll();
    }

    void activateEncoders() {
        encoderModeControls.activateCurrent();
    }

    void deactivateEncoders() {
        encoderModeControls.deactivateAll();
    }

    void handleMute1(final boolean pressed) {
        performanceControls.handleMute1(pressed);
    }

    void handleMute2(final boolean pressed) {
        performanceControls.handleMute2(pressed);
    }

    void handleMute3(final boolean pressed) {
        performanceControls.handleMute3(pressed);
    }

    BiColorLightState mute1LightState() {
        return performanceControls.mute1LightState();
    }

    BiColorLightState mute2LightState() {
        return performanceControls.mute2LightState();
    }

    BiColorLightState mute3LightState() {
        return performanceControls.mute3LightState();
    }

    void handleModeAdvance(final boolean pressed, final boolean noteStepActive) {
        if (!pressed || noteStepActive) {
            return;
        }
        encoderModeControls.advanceMode();
        detailDisplay.show("Encoder Mode", encoderModeControls.modeInfo());
    }

    BiColorLightState modeLightState() {
        return encoderModeControls.lightState();
    }

    EncoderMode currentEncoderMode() {
        return encoderModeControls.mode();
    }

    void handleExpressionTouch(final boolean touched, final String label, final String value) {
        if (touched) {
            valueDisplay.show(label, value);
        } else {
            clearDisplay.run();
        }
    }

    void handleResettableTouch(final int encoderIndex, final boolean touched,
                               final Runnable showInfo, final Runnable resetAction) {
        handleResettableTouch(encoderIndex, touched, true, "No reset", showInfo, resetAction);
    }

    void handleResettableTouch(final int encoderIndex, final boolean touched,
                               final boolean resettable,
                               final String unavailableDetail,
                               final Runnable showInfo,
                               final Runnable resetAction) {
        if (ParameterEncoderBinding.handleExplicitResetTouch(touched, explicitResetControl, resettable,
                "Reset", unavailableDetail, resetAction, showInfo, valueDisplay::show)) {
            return;
        }
        touchDisplayHandler.handleTouch(touched, showInfo);
    }

    @FunctionalInterface
    interface ValueDisplay {
        void show(String title, String detail);
    }

    @FunctionalInterface
    interface DetailDisplay {
        void show(String title, String detail);
    }
}
