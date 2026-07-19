package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipContextFeedbackTest {
    @Test
    void asksForAGroupOrChildWhenNoContainingGroupWasFound() {
        assertEquals(
                new MulticlipContextFeedback.Message("No PolySeq", "Select or name group"),
                MulticlipContextFeedback.message(
                        MulticlipGroupCursorController.Discovery.NOT_FOUND, false, 0, 0));
        assertEquals(
                new MulticlipContextFeedback.Message("Multiple PolySeq", "Select target group"),
                MulticlipContextFeedback.message(
                        MulticlipGroupCursorController.Discovery.MULTIPLE, false, 0, 0));
    }

    @Test
    void identifiesTheMissingPartOfARecognizedGroupSetup() {
        assertEquals(
                new MulticlipContextFeedback.Message("No MIDI children", "Add direct tracks"),
                MulticlipContextFeedback.message(
                        MulticlipGroupCursorController.Discovery.SELECTED, true, 0, 0));
        assertEquals(
                new MulticlipContextFeedback.Message("No MIDI children", "Use direct tracks"),
                MulticlipContextFeedback.message(
                        MulticlipGroupCursorController.Discovery.NAMED, true, 4, 0));
    }
}
