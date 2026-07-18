package com.oikoaudio.fire.multiclip;

/** Best-effort Detail Editor presentation; never part of sequencer correctness. */
public enum EditorPresentationPlan {
    NONE,
    NATIVE_GROUP,
    ACTIVE_CLIP;

    public static EditorPresentationPlan choose(
            final boolean groupExists,
            final boolean sceneExists,
            final boolean nativeContextRetained) {
        if (!groupExists) {
            return NONE;
        }
        if (sceneExists && nativeContextRetained) {
            return NATIVE_GROUP;
        }
        return ACTIVE_CLIP;
    }
}
