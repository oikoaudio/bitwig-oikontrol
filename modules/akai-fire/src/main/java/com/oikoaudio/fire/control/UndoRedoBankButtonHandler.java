package com.oikoaudio.fire.control;

/**
 * Resolves the project history gesture on physical BANK left/right buttons.
 */
public final class UndoRedoBankButtonHandler {
    private UndoRedoBankButtonHandler() {
    }

    public static Action actionFor(final boolean pressed, final int amount, final boolean altHeld) {
        if (!pressed || !altHeld || amount == 0) {
            return Action.NONE;
        }
        return amount < 0 ? Action.UNDO : Action.REDO;
    }

    public enum Action {
        NONE,
        UNDO,
        REDO
    }
}
