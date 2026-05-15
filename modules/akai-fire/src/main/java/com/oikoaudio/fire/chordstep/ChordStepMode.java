package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.note.PitchedSurfaceLayer;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;

public final class ChordStepMode extends PitchedSurfaceLayer {

    public ChordStepMode(final AkaiFireOikontrolExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver, noteRepeatHandler, "CHORD_STEP_MODE_LAYER");
    }

    @Override
    public boolean isChordStepSurface() {
        return true;
    }
}
