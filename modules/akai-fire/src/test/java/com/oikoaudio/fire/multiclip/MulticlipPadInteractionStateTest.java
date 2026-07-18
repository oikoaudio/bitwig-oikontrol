package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MulticlipPadInteractionStateTest {
    @Test
    void reportsHeldPadsOnlyWhenTheyBelongToAnotherRow() {
        final MulticlipPadInteractionState state = new MulticlipPadInteractionState();

        state.press(2);

        assertFalse(state.hasHeldPadInAnotherRow(7));
        assertTrue(state.hasHeldPadInAnotherRow(18));
    }

    @Test
    void ignoresThePadCurrentlyBeginningItsPress() {
        final MulticlipPadInteractionState state = new MulticlipPadInteractionState();

        state.press(18);

        assertFalse(state.hasHeldPadInAnotherRow(18));
    }
}
