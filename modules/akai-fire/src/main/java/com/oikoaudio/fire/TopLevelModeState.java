package com.oikoaudio.fire;

public final class TopLevelModeState {
    public enum Mode {
        DRUM,
        NOTE_PLAY,
        CHORD_STEP,
        MELODIC_STEP,
        PERFORM
    }

    public enum NoteButtonResult {
        TOGGLE_NOTE_VARIANT,
        TOGGLE_CHORD_VARIANT,
        SWITCH_TO_NOTE_PLAY,
        SWITCH_TO_CHORD_STEP
    }

    private Mode activeMode = Mode.NOTE_PLAY;
    private Mode previousNonStepMode = Mode.NOTE_PLAY;

    public Mode activeMode() {
        return activeMode;
    }

    public void activateDrum() {
        activeMode = Mode.DRUM;
    }

    public void activatePerform() {
        activeMode = Mode.PERFORM;
    }

    public NoteButtonResult handleNotePressed(final boolean altHeld) {
        if (activeMode == Mode.NOTE_PLAY) {
            if (altHeld) {
                activeMode = Mode.CHORD_STEP;
                return NoteButtonResult.SWITCH_TO_CHORD_STEP;
            }
            return NoteButtonResult.TOGGLE_NOTE_VARIANT;
        }
        if (activeMode == Mode.CHORD_STEP) {
            if (altHeld) {
                return NoteButtonResult.TOGGLE_CHORD_VARIANT;
            }
            activeMode = Mode.NOTE_PLAY;
            return NoteButtonResult.SWITCH_TO_NOTE_PLAY;
        }
        activeMode = Mode.NOTE_PLAY;
        return NoteButtonResult.SWITCH_TO_NOTE_PLAY;
    }

    public boolean shouldIgnoreTopLevelStepPress(final boolean shiftHeld, final boolean altHeld) {
        return (activeMode == Mode.DRUM || activeMode == Mode.NOTE_PLAY
                || activeMode == Mode.CHORD_STEP || activeMode == Mode.PERFORM)
                && (shiftHeld || altHeld);
    }

    public boolean isChordStepActive() {
        return activeMode == Mode.CHORD_STEP;
    }

    public void enterMelodicStepMode() {
        previousNonStepMode = activeMode == Mode.MELODIC_STEP ? Mode.NOTE_PLAY : activeMode;
        activeMode = Mode.MELODIC_STEP;
    }

    public Mode exitMelodicStepMode() {
        activeMode = previousNonStepMode == Mode.MELODIC_STEP ? Mode.NOTE_PLAY : previousNonStepMode;
        return activeMode;
    }
}
