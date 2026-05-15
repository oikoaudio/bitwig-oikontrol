package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.note.PitchedSurfaceLayer;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;

/**
 * Public chord-step mode shell.
 *
 * <p>The current implementation still delegates to a legacy pitched-surface adapter while chord-step ownership moves
 * out of {@link PitchedSurfaceLayer}. Keeping that adapter private prevents new callers from depending on chord step as
 * a live-note surface.</p>
 */
public final class ChordStepMode extends Layer {
    private final LegacyChordStepSurface legacySurface;

    public ChordStepMode(final AkaiFireOikontrolExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver.getLayers(), "CHORD_STEP_MODE");
        legacySurface = new LegacyChordStepSurface(driver, noteRepeatHandler);
    }

    public void notifyBlink(final int blinkTicks) {
        legacySurface.notifyBlink(blinkTicks);
    }

    public BiColorLightState getModeButtonLightState() {
        return legacySurface.getModeButtonLightState();
    }

    public void toggleSurfaceVariant() {
        legacySurface.toggleSurfaceVariant();
    }

    @Override
    protected void onActivate() {
        legacySurface.activate();
    }

    @Override
    protected void onDeactivate() {
        legacySurface.deactivate();
    }

    private static final class LegacyChordStepSurface extends PitchedSurfaceLayer {
        private LegacyChordStepSurface(final AkaiFireOikontrolExtension driver,
                                       final NoteRepeatHandler noteRepeatHandler) {
            super(driver, noteRepeatHandler, "CHORD_STEP_MODE_LAYER");
        }

        @Override
        protected boolean isChordStepSurface() {
            return true;
        }
    }
}
