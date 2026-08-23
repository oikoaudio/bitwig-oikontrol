package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipGridGestureTest {
    @Test
    void resolvesDocumentedModifierPrecedence() {
        assertEquals(
                MulticlipGridGesture.PLAY_START, MulticlipGridGesture.resolve(false, false, false));
        assertEquals(
                MulticlipGridGesture.PLAY_START_FINE,
                MulticlipGridGesture.resolve(true, false, false));
        assertEquals(
                MulticlipGridGesture.HELD_STEP_NUDGE,
                MulticlipGridGesture.resolve(false, false, true));
        assertEquals(
                MulticlipGridGesture.HELD_STEP_NUDGE,
                MulticlipGridGesture.resolve(true, false, true));
        assertEquals(
                MulticlipGridGesture.CLIP_LENGTH, MulticlipGridGesture.resolve(false, true, true));
        assertEquals(
                MulticlipGridGesture.WHOLE_LANE_NUDGE,
                MulticlipGridGesture.resolve(true, true, true));
        assertEquals(
                MulticlipGridGesture.WHOLE_LANE_NUDGE,
                MulticlipGridGesture.resolve(true, true, false));
    }
}
