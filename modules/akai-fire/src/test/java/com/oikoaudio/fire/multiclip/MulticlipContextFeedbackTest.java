package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipContextFeedbackTest {
    @Test
    void asksForAGroupOrChildWhenNoContainingGroupWasFound() {
        assertEquals(
                new MulticlipContextFeedback.Message("Setup not found", "Select group/child"),
                MulticlipContextFeedback.message(false, false, 0, 0));
    }

    @Test
    void identifiesTheMissingPartOfARecognizedGroupSetup() {
        assertEquals(
                new MulticlipContextFeedback.Message("No Drum Machine", "Add to target group"),
                MulticlipContextFeedback.message(true, false, 4, 4));
        assertEquals(
                new MulticlipContextFeedback.Message("No MIDI children", "Add direct tracks"),
                MulticlipContextFeedback.message(true, true, 0, 0));
        assertEquals(
                new MulticlipContextFeedback.Message("No MIDI children", "Use direct tracks"),
                MulticlipContextFeedback.message(true, true, 4, 0));
    }
}
