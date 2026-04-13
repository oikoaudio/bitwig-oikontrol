package com.oikoaudio.fire.note;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;

public final class NotePlayMode extends PitchedSurfaceLayer {

    public NotePlayMode(final AkaiFireOikontrolExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver, noteRepeatHandler, "NOTE_PLAY_MODE_LAYER", SurfaceRole.NOTE_PLAY);
    }
}
