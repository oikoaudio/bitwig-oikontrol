package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.NoteStep;

record MulticlipNoteDefaults(double pressure, double timbre) {
    void applyTo(final NoteStep note) {
        note.setPressure(pressure);
        note.setTimbre(timbre);
    }
}
