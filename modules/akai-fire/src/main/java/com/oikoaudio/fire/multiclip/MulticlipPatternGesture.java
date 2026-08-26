package com.oikoaudio.fire.multiclip;

/** Resolves whether Multiclip Pattern buttons navigate time or scenes. */
enum MulticlipPatternGesture {
    TIME_PAGE,
    SCENE_PAGE;

    static MulticlipPatternGesture resolve(final boolean shiftHeld) {
        return shiftHeld ? SCENE_PAGE : TIME_PAGE;
    }
}
