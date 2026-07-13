package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.NoteInput;
import com.oikoaudio.fire.display.OledDisplay;
import org.junit.jupiter.api.Test;

class NoteRepeatHandlerTest {

    @Test
    void heldButtonRateAdjustmentPreservesActivationAndBecomesTheNextToggleRate() {
        final NoteInput noteInput = mock(NoteInput.class, RETURNS_DEEP_STUBS);
        final Arpeggiator arpeggiator = noteInput.arpeggiator();
        final NoteRepeatHandler handler =
                new NoteRepeatHandler(noteInput, mock(OledDisplay.class), () -> null, () -> 100);

        handler.adjustRatePreservingActivation(1);

        assertFalse(handler.getNoteRepeatActive().get());
        handler.toggleActive();
        assertTrue(handler.getNoteRepeatActive().get());
        verify(arpeggiator.rate()).set(0.5);

        handler.adjustRatePreservingActivation(1);

        assertTrue(handler.getNoteRepeatActive().get());
        verify(arpeggiator.rate()).set(1.0);

        handler.toggleActive();
        assertFalse(handler.getNoteRepeatActive().get());
        handler.toggleActive();

        assertTrue(handler.getNoteRepeatActive().get());
        verify(arpeggiator.rate(), times(2)).set(1.0);
    }
}
