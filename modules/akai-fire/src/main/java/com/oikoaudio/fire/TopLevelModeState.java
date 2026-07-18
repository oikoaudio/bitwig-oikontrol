package com.oikoaudio.fire;

public final class TopLevelModeState {
    public enum Mode {
        DRUM,
        NOTE_PLAY,
        CHORD_STEP,
        MELODIC_STEP,
        FUGUE_STEP,
        PERFORM
    }

    public enum DrumMode {
        STANDARD,
        MULTICLIP_SEQ,
        NESTED_RHYTHM,
        DRUM_PADS;

        public DrumMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean usesAutoPinnedDrumContext() {
            return this == STANDARD;
        }

        public boolean takesOverAutoPinnedDrumSelection() {
            return this == MULTICLIP_SEQ;
        }
    }

    public enum PerformMode {
        LAUNCHER,
        MIX
    }

    private Mode activeMode = Mode.NOTE_PLAY;
    private Mode previousNonStepMode = Mode.NOTE_PLAY;
    private Mode rememberedStepMode = Mode.CHORD_STEP;
    private DrumMode activeDrumMode = DrumMode.STANDARD;
    private PerformMode activePerformMode = PerformMode.LAUNCHER;

    public Mode activeMode() {
        return activeMode;
    }

    public void activateDrum() {
        activeMode = Mode.DRUM;
    }

    public void activateDrum(final DrumMode mode) {
        activeDrumMode = mode;
        activeMode = Mode.DRUM;
    }

    public DrumMode cycleDrumMode() {
        activeDrumMode = activeDrumMode.next();
        activeMode = Mode.DRUM;
        return activeDrumMode;
    }

    public DrumMode activeDrumMode() {
        return activeDrumMode;
    }

    public void activatePerform() {
        activeMode = Mode.PERFORM;
    }

    public void activatePerform(final PerformMode mode) {
        activePerformMode = mode;
        activeMode = Mode.PERFORM;
    }

    public PerformMode activePerformMode() {
        return activePerformMode;
    }

    public void activateNotePlay() {
        activeMode = Mode.NOTE_PLAY;
    }

    public void activateChordStep() {
        rememberedStepMode = Mode.CHORD_STEP;
        activeMode = Mode.CHORD_STEP;
    }

    public void activateFugueStep() {
        rememberedStepMode = Mode.FUGUE_STEP;
        activeMode = Mode.FUGUE_STEP;
    }

    public boolean shouldIgnoreTopLevelStepPress(final boolean shiftHeld, final boolean altHeld) {
        return (activeMode == Mode.DRUM
                        || activeMode == Mode.NOTE_PLAY
                        || activeMode == Mode.CHORD_STEP
                        || activeMode == Mode.PERFORM)
                && (shiftHeld || altHeld);
    }

    public boolean isChordStepActive() {
        return activeMode == Mode.CHORD_STEP;
    }

    public void enterMelodicStepMode() {
        previousNonStepMode = isStepFamily(activeMode) ? Mode.NOTE_PLAY : activeMode;
        rememberedStepMode = Mode.MELODIC_STEP;
        activeMode = Mode.MELODIC_STEP;
    }

    public Mode exitMelodicStepMode() {
        activeMode =
                previousNonStepMode == Mode.MELODIC_STEP ? Mode.NOTE_PLAY : previousNonStepMode;
        return activeMode;
    }

    public Mode plainStepPressTarget() {
        return switch (activeMode) {
            case CHORD_STEP -> Mode.MELODIC_STEP;
            case MELODIC_STEP -> Mode.FUGUE_STEP;
            case FUGUE_STEP -> Mode.CHORD_STEP;
            default -> rememberedStepMode;
        };
    }

    private boolean isStepFamily(final Mode mode) {
        return mode == Mode.CHORD_STEP || mode == Mode.MELODIC_STEP || mode == Mode.FUGUE_STEP;
    }
}
