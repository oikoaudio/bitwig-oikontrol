package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoteClipAvailabilityTest {

    @Test
    void selectedClipSlotFailsOnAudioTrack() {
        final NoteClipAvailability.Failure failure = NoteClipAvailability.requireSelectedClipSlot(false, true);

        assertEquals("Audio Track", failure.title());
        assertEquals("Use note track", failure.oledDetail());
        assertEquals("Use note track", failure.popupDetail());
    }

    @Test
    void selectedClipSlotFailsWhenNoClipIsSelected() {
        final NoteClipAvailability.Failure failure = NoteClipAvailability.requireSelectedClipSlot(true, false);

        assertEquals("No Clip", failure.title());
        assertEquals("Select clip", failure.oledDetail());
        assertEquals("Select clip", failure.popupDetail());
    }

    @Test
    void selectedClipSlotSucceedsWhenTrackAndSelectionAreValid() {
        assertNull(NoteClipAvailability.requireSelectedClipSlot(true, true));
    }

    @Test
    void clipContentFailsWhenNothingIsAvailable() {
        final NoteClipAvailability.Failure failure = NoteClipAvailability.requireClipContent(false);

        assertEquals("No Clip", failure.title());
        assertEquals("Create or Select Clip", failure.oledDetail());
        assertEquals("Create or select clip", failure.popupDetail());
    }

    @Test
    void clipContentSucceedsWhenContentExists() {
        assertNull(NoteClipAvailability.requireClipContent(true));
    }
}
