package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipEditButtonFeedbackTest {
    @Test
    void describesEachPrimaryMuteButtonFunctionAndTarget() {
        assertEquals(
                new MulticlipEditButtonFeedback.Message("Select", "Scene pad"),
                MulticlipEditButtonFeedback.message(0, false));
        assertEquals(
                new MulticlipEditButtonFeedback.Message("Last Step", "Pattern pad"),
                MulticlipEditButtonFeedback.message(1, false));
        assertEquals(
                new MulticlipEditButtonFeedback.Message("Paste Lane Clip", "Scene pad"),
                MulticlipEditButtonFeedback.message(2, false));
        assertEquals(
                new MulticlipEditButtonFeedback.Message("Delete", "Pattern pad"),
                MulticlipEditButtonFeedback.message(3, false));
    }

    @Test
    void describesShiftCopyAsAWholeChildScenePaste() {
        assertEquals(
                new MulticlipEditButtonFeedback.Message("Paste Child Scene", "Scene pad"),
                MulticlipEditButtonFeedback.message(2, true));
    }
}
