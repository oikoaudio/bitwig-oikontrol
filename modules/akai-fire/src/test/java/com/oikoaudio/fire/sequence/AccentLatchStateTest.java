package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccentLatchStateTest {

    @Test
    void togglesOnReleaseWhenUnmodified() {
        final AccentLatchState state = new AccentLatchState();

        assertEquals(AccentLatchState.Transition.PRESSED, state.handlePressed(true));
        assertTrue(state.isHeld());
        assertFalse(state.isActive());

        assertEquals(AccentLatchState.Transition.TOGGLED_ON_RELEASE, state.handlePressed(false));
        assertFalse(state.isHeld());
        assertTrue(state.isActive());
    }

    @Test
    void modifiedPressDoesNotToggleOnRelease() {
        final AccentLatchState state = new AccentLatchState();

        state.handlePressed(true);
        state.markModified();

        assertEquals(AccentLatchState.Transition.MODIFIED_RELEASE, state.handlePressed(false));
        assertFalse(state.isHeld());
        assertFalse(state.isActive());
    }

    @Test
    void clearsHeldStateWithoutChangingLatch() {
        final AccentLatchState state = new AccentLatchState();

        state.handlePressed(true);
        state.clearHeld();

        assertFalse(state.isHeld());
        assertFalse(state.isActive());
    }
}
