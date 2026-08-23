package com.oikoaudio.fire.multiclip;

/** Deterministic resolution of mode-local Grid button modifiers. */
public enum MulticlipGridGesture {
    HELD_STEP_NUDGE,
    PLAY_START,
    PLAY_START_FINE,
    CLIP_LENGTH,
    WHOLE_LANE_NUDGE;

    public static MulticlipGridGesture resolve(
            final boolean shiftHeld, final boolean altHeld, final boolean hasHeldSteps) {
        if (shiftHeld && altHeld) {
            return WHOLE_LANE_NUDGE;
        }
        if (altHeld) {
            return CLIP_LENGTH;
        }
        if (hasHeldSteps) {
            return HELD_STEP_NUDGE;
        }
        return shiftHeld ? PLAY_START_FINE : PLAY_START;
    }
}
