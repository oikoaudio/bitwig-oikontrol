package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NoteVariationGestureTest {

    @Test
    void combinedModifiersOwnTurnsAndTouches() {
        assertEquals(
                NoteVariationGesture.Action.ADJUST_AMOUNT, NoteVariationGesture.turn(true, true));
        assertEquals(
                NoteVariationGesture.Action.APPLY, NoteVariationGesture.touch(true, true, false));
    }

    @Test
    void knobModeTouchRetainsPriorityOverCombinedModifiers() {
        assertEquals(
                NoteVariationGesture.Action.RESET_TARGET,
                NoteVariationGesture.touch(true, true, true));
    }

    @Test
    void ordinaryModifierCombinationsRemainDistinct() {
        assertEquals(NoteVariationGesture.Action.ORDINARY, NoteVariationGesture.turn(false, false));
        assertEquals(NoteVariationGesture.Action.FINE, NoteVariationGesture.turn(true, false));
        assertEquals(NoteVariationGesture.Action.ALTERNATE, NoteVariationGesture.turn(false, true));
        assertEquals(
                NoteVariationGesture.Action.ORDINARY,
                NoteVariationGesture.touch(false, false, false));
    }
}
