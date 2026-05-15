package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrumStepPadStateTest {
    @Test
    void tracksHeldSteps() {
        final DrumStepPadState state = new DrumStepPadState();

        assertFalse(state.isAnyStepHeld());

        state.addHeldStep(3);

        assertTrue(state.isAnyStepHeld());
        assertTrue(state.heldStepStream().toList().contains(3));

        state.removeHeldStep(3);

        assertFalse(state.isAnyStepHeld());
    }

    @Test
    void consumesGestureMarkersOnce() {
        final DrumStepPadState state = new DrumStepPadState();

        state.markGestureConsumed(7);

        assertTrue(state.consumeGesture(7));
        assertFalse(state.consumeGesture(7));
    }

    @Test
    void separatesAddedAndModifiedStepFlags() {
        final DrumStepPadState state = new DrumStepPadState();

        state.markAdded(4);
        state.markModified(4);

        assertTrue(state.isAdded(4));
        assertTrue(state.isModified(4));

        state.removeAdded(4);
        state.removeModified(4);

        assertFalse(state.isAdded(4));
        assertFalse(state.isModified(4));
    }
}
