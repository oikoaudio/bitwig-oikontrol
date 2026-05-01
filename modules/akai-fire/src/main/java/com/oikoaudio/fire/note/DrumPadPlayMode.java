package com.oikoaudio.fire.note;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;

public final class DrumPadPlayMode extends PitchedSurfaceLayer {

    public DrumPadPlayMode(final AkaiFireOikontrolExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver, noteRepeatHandler, "DRUM_PAD_PLAY_MODE_LAYER", SurfaceRole.NOTE_PLAY, true);
    }
}
