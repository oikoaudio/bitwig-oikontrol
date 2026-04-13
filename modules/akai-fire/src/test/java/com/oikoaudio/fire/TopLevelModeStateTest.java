package com.oikoaudio.fire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopLevelModeStateTest {

    @Test
    void noteButtonSwitchesBetweenNotePlayAndChordStep() {
        final TopLevelModeState state = new TopLevelModeState();

        assertEquals(TopLevelModeState.NoteButtonResult.SWITCH_TO_CHORD_STEP, state.handleNotePressed(false));
        assertEquals(TopLevelModeState.Mode.CHORD_STEP, state.activeMode());

        assertEquals(TopLevelModeState.NoteButtonResult.SWITCH_TO_NOTE_PLAY, state.handleNotePressed(false));
        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.activeMode());
    }

    @Test
    void alternateNoteButtonDoesNotLeaveCurrentNoteSurface() {
        final TopLevelModeState state = new TopLevelModeState();

        assertEquals(TopLevelModeState.NoteButtonResult.TOGGLE_NOTE_VARIANT, state.handleNotePressed(true));
        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.activeMode());

        state.handleNotePressed(false);
        assertEquals(TopLevelModeState.NoteButtonResult.TOGGLE_CHORD_VARIANT, state.handleNotePressed(true));
        assertEquals(TopLevelModeState.Mode.CHORD_STEP, state.activeMode());
    }

    @Test
    void melodicExitReturnsToPreviousNonStepMode() {
        final TopLevelModeState state = new TopLevelModeState();
        state.handleNotePressed(false);

        state.enterMelodicStepMode();

        assertEquals(TopLevelModeState.Mode.MELODIC_STEP, state.activeMode());
        assertEquals(TopLevelModeState.Mode.CHORD_STEP, state.exitMelodicStepMode());
        assertEquals(TopLevelModeState.Mode.CHORD_STEP, state.activeMode());
    }

    @Test
    void melodicExitFallsBackToNotePlayIfPreviousWasAlreadyMelodic() {
        final TopLevelModeState state = new TopLevelModeState();

        state.enterMelodicStepMode();
        state.enterMelodicStepMode();

        assertEquals(TopLevelModeState.Mode.NOTE_PLAY, state.exitMelodicStepMode());
    }

    @Test
    void topLevelStepPressIgnoreMatchesModifierRules() {
        final TopLevelModeState state = new TopLevelModeState();

        assertTrue(state.shouldIgnoreTopLevelStepPress(true, false));
        assertTrue(state.shouldIgnoreTopLevelStepPress(false, true));
        assertFalse(state.shouldIgnoreTopLevelStepPress(false, false));
    }
}
