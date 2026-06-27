package com.oikoaudio.fire.control;

/**
 * Shared touch display orchestration for encoder interactions.
 */
public final class EncoderTouchDisplayHandler {
    private final Runnable clearDisplay;

    public EncoderTouchDisplayHandler(final Runnable clearDisplay) {
        this.clearDisplay = clearDisplay;
    }

    public void handleTouch(final boolean touched, final Runnable showInfo) {
        if (touched) {
            showInfo.run();
            return;
        }
        clearDisplay.run();
    }
}
