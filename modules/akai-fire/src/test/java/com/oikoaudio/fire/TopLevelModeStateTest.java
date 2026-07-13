package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TopLevelModeStateTest {

    @Test
    void canActivateLiveAndChordNoteModesExplicitly() {
        final TopLevelModeState state = new TopLevelModeState();

        state.activateChordStep();
        assertEquals(TopLevelModeState.Mode.CHORD_STEP, state.activeMode());

        state.activateNotePlay();
        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.activeMode());
    }

    @Test
    void melodicExitFallsBackToNotePlayIfPreviousWasChordStep() {
        final TopLevelModeState state = new TopLevelModeState();
        state.activateChordStep();

        state.enterMelodicStepMode();

        assertEquals(TopLevelModeState.Mode.MELODIC_STEP, state.activeMode());
        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.exitMelodicStepMode());
        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.activeMode());
    }

    @Test
    void melodicExitFallsBackToNotePlayIfPreviousWasAlreadyMelodic() {
        final TopLevelModeState state = new TopLevelModeState();

        state.enterMelodicStepMode();
        state.enterMelodicStepMode();

        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.exitMelodicStepMode());
    }

    @Test
    void melodicExitFallsBackToNotePlayIfPreviousWasFugueStep() {
        final TopLevelModeState state = new TopLevelModeState();

        state.activateFugueStep();
        state.enterMelodicStepMode();

        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.exitMelodicStepMode());
    }

    @Test
    void plainStepPressCycleStartsWithChordStepThenMelodicGeneratorThenFugue() {
        final TopLevelModeState state = new TopLevelModeState();

        assertEquals(TopLevelModeState.Mode.CHORD_STEP, state.plainStepPressTarget());

        state.activateChordStep();
        assertEquals(TopLevelModeState.Mode.MELODIC_STEP, state.plainStepPressTarget());

        state.enterMelodicStepMode();
        assertEquals(TopLevelModeState.Mode.FUGUE_STEP, state.plainStepPressTarget());

        state.activateFugueStep();
        assertEquals(TopLevelModeState.Mode.CHORD_STEP, state.plainStepPressTarget());
    }

    @Test
    void stepButtonRestoresLastStepModeWhenReEnteringStepFamily() {
        final TopLevelModeState state = new TopLevelModeState();

        state.activateFugueStep();
        state.activatePerform();

        assertEquals(TopLevelModeState.Mode.FUGUE_STEP, state.plainStepPressTarget());
    }

    @Test
    void drumButtonRestoresLastDrumModeWhenReEnteringDrumFamily() {
        final TopLevelModeState state = new TopLevelModeState();

        assertEquals(TopLevelModeState.DrumMode.STANDARD, state.activeDrumMode());
        state.cycleDrumMode();
        assertEquals(TopLevelModeState.DrumMode.NESTED_RHYTHM, state.activeDrumMode());

        state.activatePerform();
        state.activateDrum();

        assertEquals(TopLevelModeState.Mode.DRUM, state.activeMode());
        assertEquals(TopLevelModeState.DrumMode.NESTED_RHYTHM, state.activeDrumMode());
    }

    @Test
    void performButtonRestoresLastPerformModeWhenReEnteringPerformFamily() {
        final TopLevelModeState state = new TopLevelModeState();

        assertEquals(TopLevelModeState.PerformMode.LAUNCHER, state.activePerformMode());

        state.activatePerform(TopLevelModeState.PerformMode.MIX);
        state.activateDrum();
        state.activatePerform();

        assertEquals(TopLevelModeState.Mode.PERFORM, state.activeMode());
        assertEquals(TopLevelModeState.PerformMode.MIX, state.activePerformMode());
    }

    @Test
    void startupCanSetPerformDefaultWithoutLosingLauncherDefaultForOtherSessions() {
        final TopLevelModeState state = new TopLevelModeState();

        state.activatePerform(TopLevelModeState.PerformMode.MIX);

        assertEquals(TopLevelModeState.Mode.PERFORM, state.activeMode());
        assertEquals(TopLevelModeState.PerformMode.MIX, state.activePerformMode());
    }

    @Test
    void topLevelStepPressIgnoreMatchesModifierRules() {
        final TopLevelModeState state = new TopLevelModeState();

        assertTrue(state.shouldIgnoreTopLevelStepPress(true, false));
        assertTrue(state.shouldIgnoreTopLevelStepPress(false, true));
        assertFalse(state.shouldIgnoreTopLevelStepPress(false, false));
    }
}
