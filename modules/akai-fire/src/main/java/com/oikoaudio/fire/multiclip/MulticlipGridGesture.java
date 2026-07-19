package com.oikoaudio.fire.multiclip;

/** Deterministic resolution of mode-local Grid button modifiers. */
public enum MulticlipGridGesture {
    TIME_PAGE,
    HELD_STEP_NUDGE,
    PLAY_START,
    WHOLE_LANE_NUDGE;

    public static MulticlipGridGesture resolve(
            final boolean shiftHeld, final boolean altHeld, final boolean hasHeldSteps) {
        if (shiftHeld && altHeld) {
            return WHOLE_LANE_NUDGE;
        }
        if (altHeld) {
            return PLAY_START;
        }
        if (hasHeldSteps) {
            return HELD_STEP_NUDGE;
        }
        return TIME_PAGE;
    }
}
