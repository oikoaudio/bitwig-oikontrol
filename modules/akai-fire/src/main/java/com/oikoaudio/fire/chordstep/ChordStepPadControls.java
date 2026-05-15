package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.RgbLigthState;

/**
 * Owns chord-step pad input and light routing for the 64-pad surface.
 */
public final class ChordStepPadControls {
    private final ChordStepPadController controller;
    private final ChordStepPadLightRenderer lightRenderer;
    private final int clipRowPadCount;
    private final int chordSourcePadOffset;
    private final int stepPadOffset;

    public ChordStepPadControls(final ChordStepPadController controller,
                                final ChordStepPadLightRenderer lightRenderer,
                                final int clipRowPadCount,
                                final int chordSourcePadOffset,
                                final int stepPadOffset) {
        this.controller = controller;
        this.lightRenderer = lightRenderer;
        this.clipRowPadCount = clipRowPadCount;
        this.chordSourcePadOffset = chordSourcePadOffset;
        this.stepPadOffset = stepPadOffset;
    }

    public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
        controller.handlePadPress(padIndex, pressed, velocity);
    }

    public RgbLigthState padLight(final int padIndex) {
        return lightRenderer.padLight(padIndex, clipRowPadCount, chordSourcePadOffset, stepPadOffset);
    }
}
