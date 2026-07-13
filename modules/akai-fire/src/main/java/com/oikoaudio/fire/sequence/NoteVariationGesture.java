package com.oikoaudio.fire.sequence;

/** Shared modifier precedence for Baked Note Variation encoder gestures. */
public final class NoteVariationGesture {
    private NoteVariationGesture() {}

    public static Action turn(final boolean shiftHeld, final boolean altHeld) {
        if (shiftHeld && altHeld) {
            return Action.ADJUST_AMOUNT;
        }
        if (shiftHeld) {
            return Action.FINE;
        }
        return altHeld ? Action.ALTERNATE : Action.ORDINARY;
    }

    public static Action touch(
            final boolean shiftHeld, final boolean altHeld, final boolean knobModeHeld) {
        if (knobModeHeld) {
            return Action.RESET_TARGET;
        }
        if (shiftHeld && altHeld) {
            return Action.APPLY;
        }
        return altHeld ? Action.ALTERNATE : Action.ORDINARY;
    }

    public enum Action {
        ORDINARY,
        FINE,
        ALTERNATE,
        RESET_TARGET,
        ADJUST_AMOUNT,
        APPLY
    }
}
