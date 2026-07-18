package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MulticlipPadInteractionStateTest {
    @Test
    void consumesHeldStepsAcrossBothPatternRows() {
        final MulticlipPadInteractionState state = new MulticlipPadInteractionState();

        state.press(34);
        state.press(51);
        state.consumeHeldPattern();

        assertTrue(state.isConsumed(34));
        assertTrue(state.isConsumed(51));
    }

    @Test
    void doesNotConsumeSceneOrLanePads() {
        final MulticlipPadInteractionState state = new MulticlipPadInteractionState();

        state.press(2);
        state.press(18);
        state.consumeHeldPattern();

        assertFalse(state.isConsumed(2));
        assertFalse(state.isConsumed(18));
    }
}
