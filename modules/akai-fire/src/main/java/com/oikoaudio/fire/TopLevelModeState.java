package com.oikoaudio.fire;

public final class TopLevelModeState {
    public enum Mode {
        DRUM,
        NOTE_PLAY,
        CHORD_STEP,
        MELODIC_STEP,
        NESTED_RHYTHM,
        PERFORM
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

    public void activateNotePlay() {
        activeMode = Mode.NOTE_PLAY;
    }

    public void activateChordStep() {
        activeMode = Mode.CHORD_STEP;
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
        previousNonStepMode = (activeMode == Mode.MELODIC_STEP || activeMode == Mode.NESTED_RHYTHM)
                ? Mode.NOTE_PLAY
                : activeMode;
        activeMode = Mode.MELODIC_STEP;
    }

    public Mode exitMelodicStepMode() {
        activeMode = previousNonStepMode == Mode.MELODIC_STEP ? Mode.NOTE_PLAY : previousNonStepMode;
        return activeMode;
    }

    public void enterNestedRhythmMode() {
        previousNonStepMode = (activeMode == Mode.MELODIC_STEP || activeMode == Mode.NESTED_RHYTHM)
                ? Mode.NOTE_PLAY
                : activeMode;
        activeMode = Mode.NESTED_RHYTHM;
    }
}
