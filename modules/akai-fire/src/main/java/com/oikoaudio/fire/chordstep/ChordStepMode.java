package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;

/**
 * Public chord-step mode shell.
 */
public final class ChordStepMode extends Layer {
    private final ChordStepSurfaceLayer surfaceLayer;

    public ChordStepMode(final AkaiFireOikontrolExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver.getLayers(), "CHORD_STEP_MODE");
        surfaceLayer = new ChordStepSurfaceLayer(driver, noteRepeatHandler, "CHORD_STEP_MODE_LAYER");
    }

    public void notifyBlink(final int blinkTicks) {
        surfaceLayer.notifyBlink(blinkTicks);
    }

    public BiColorLightState getModeButtonLightState() {
        return surfaceLayer.getModeButtonLightState();
    }

    public void toggleSurfaceVariant() {
        surfaceLayer.toggleSurfaceVariant();
    }

    @Override
    protected void onActivate() {
        surfaceLayer.activate();
    }

    @Override
    protected void onDeactivate() {
        surfaceLayer.deactivate();
    }
}
