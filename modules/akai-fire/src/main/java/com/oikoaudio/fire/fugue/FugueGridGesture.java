package com.oikoaudio.fire.fugue;

enum FugueGridGesture {
    SOURCE_PLAY_START,
    DERIVED_LINE_START,
    CLIP_LENGTH;

    static FugueGridGesture resolve(final boolean sourceLine, final boolean altHeld) {
        if (altHeld) {
            return CLIP_LENGTH;
        }
        return sourceLine ? SOURCE_PLAY_START : DERIVED_LINE_START;
    }
}
