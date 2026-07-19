package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipGridGestureTest {
    @Test
    void resolvesDocumentedModifierPrecedence() {
        assertEquals(
                MulticlipGridGesture.TIME_PAGE, MulticlipGridGesture.resolve(false, false, false));
        assertEquals(
                MulticlipGridGesture.HELD_STEP_NUDGE,
                MulticlipGridGesture.resolve(false, false, true));
        assertEquals(
                MulticlipGridGesture.PLAY_START, MulticlipGridGesture.resolve(false, true, true));
        assertEquals(
                MulticlipGridGesture.WHOLE_LANE_NUDGE,
                MulticlipGridGesture.resolve(true, true, true));
    }
}
