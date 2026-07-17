package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.NoteInput;
import com.oikoaudio.fire.display.OledDisplay;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NoteRepeatHandlerTest {

    @Test
    void repeatRatesAdvanceMonotonicallyFromFastestToSlowest() {
        final NoteInput noteInput = mock(NoteInput.class, RETURNS_DEEP_STUBS);
        final Arpeggiator arpeggiator = noteInput.arpeggiator();
        final OledDisplay oled = mock(OledDisplay.class);
        final NoteRepeatHandler handler =
                new NoteRepeatHandler(noteInput, oled, () -> null, () -> 100);

        handler.toggleActive();
        for (int i = 0; i < 3; i++) {
            handler.adjustRatePreservingActivation(-1);
        }
        for (int i = 0; i < 7; i++) {
            handler.adjustRatePreservingActivation(1);
        }

        final ArgumentCaptor<Double> rate = ArgumentCaptor.forClass(Double.class);
        verify(arpeggiator.rate(), times(11)).set(rate.capture());
        assertEquals(
                List.of(
                        0.25, 1.0 / 6, 0.125, 1.0 / 12, 0.125, 1.0 / 6, 0.25, 1.0 / 3, 0.5, 2.0 / 3,
                        1.0),
                rate.getAllValues());

        final ArgumentCaptor<String> label = ArgumentCaptor.forClass(String.class);
        verify(oled, times(11)).valueInfo(eq("Note Repeat"), label.capture());
        assertEquals(
                List.of(
                        "1/16", "1/16T", "1/32", "1/32T", "1/32", "1/16T", "1/16", "1/8T", "1/8",
                        "1/4T", "1/4"),
                label.getAllValues());
    }

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
        verify(arpeggiator.rate()).set(1.0 / 3);

        handler.adjustRatePreservingActivation(1);

        assertTrue(handler.getNoteRepeatActive().get());
        verify(arpeggiator.rate()).set(0.5);

        handler.toggleActive();
        assertFalse(handler.getNoteRepeatActive().get());
        handler.toggleActive();

        assertTrue(handler.getNoteRepeatActive().get());
        verify(arpeggiator.rate(), times(2)).set(0.5);
    }
}
