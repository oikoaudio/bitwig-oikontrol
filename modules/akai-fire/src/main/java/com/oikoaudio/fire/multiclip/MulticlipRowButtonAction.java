package com.oikoaudio.fire.multiclip;

enum MulticlipRowButtonAction {
    SELECT,
    SOLO,
    MUTE;

    static MulticlipRowButtonAction resolve(final boolean altHeld, final boolean shiftHeld) {
        if (altHeld) {
            return SELECT;
        }
        return shiftHeld ? SOLO : MUTE;
    }
}
