package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepSelectedClipStateTest {

    @Test
    void refreshReportsWhetherSelectionOrContentChanged() {
        final ChordStepSelectedClipState state = new ChordStepSelectedClipState();
        final RgbLigthState color = RgbLigthState.GRAY_1;

        assertTrue(state.refresh(selectedState(2, true, color)));
        assertFalse(state.refresh(selectedState(2, true, color)));
        assertTrue(state.refresh(selectedState(2, false, color)));
    }

    @Test
    void refreshStoresLatestSlotContentAndColor() {
        final ChordStepSelectedClipState state = new ChordStepSelectedClipState();
        final RgbLigthState color = RgbLigthState.GRAY_2;

        state.refresh(selectedState(3, true, color));

        assertEquals(3, state.slotIndex());
        assertTrue(state.hasContent());
        assertSame(color, state.color());
    }

    @Test
    void requireSelectedClipSlotDelegatesAvailabilityCheck() {
        final ChordStepSelectedClipState state = new ChordStepSelectedClipState();

        final NoteClipAvailability.Failure failure = state.requireSelectedClipSlot(true);

        assertNotNull(failure);
        assertEquals("No Clip", failure.title());
    }

    @Test
    void requireClipReturnsTrackFailureBeforeClipChecks() {
        final ChordStepSelectedClipState state = new ChordStepSelectedClipState();
        state.refresh(selectedState(1, true, RgbLigthState.GRAY_1));

        final NoteClipAvailability.Failure failure = state.requireClip(false, true);

        assertNotNull(failure);
        assertEquals("Audio Track", failure.title());
    }

    @Test
    void requireClipAcceptsLoadedContentEvenIfSelectionSaysEmpty() {
        final ChordStepSelectedClipState state = new ChordStepSelectedClipState();
        state.refresh(selectedState(1, false, RgbLigthState.GRAY_1));

        assertNull(state.requireClip(true, true));
    }

    @Test
    void requireClipRejectsWhenNoClipContentIsAvailable() {
        final ChordStepSelectedClipState state = new ChordStepSelectedClipState();
        state.refresh(selectedState(1, false, RgbLigthState.GRAY_1));

        final NoteClipAvailability.Failure failure = state.requireClip(true, false);

        assertNotNull(failure);
        assertEquals("No Clip", failure.title());
    }

    private static SelectedClipSlotState selectedState(final int slotIndex,
                                                       final boolean hasContent,
                                                       final RgbLigthState color) {
        return SelectedClipSlotState.fromValues(slotIndex, hasContent, color);
    }
}
