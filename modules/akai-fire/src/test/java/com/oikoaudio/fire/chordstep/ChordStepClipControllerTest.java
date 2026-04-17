package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepClipControllerTest {

    @Test
    void refreshQueuesResyncOnlyWhenStateChanges() {
        final List<String> events = new ArrayList<>();
        final ChordStepClipController controller = new ChordStepClipController(
                () -> true,
                () -> false,
                () -> events.add("resync"),
                failure -> events.add("failure:" + failure.title()));

        controller.refresh(SelectedClipSlotState.fromValues(1, true, RgbLigthState.GRAY_2));
        controller.refresh(SelectedClipSlotState.fromValues(1, true, RgbLigthState.GRAY_2));
        controller.refresh(SelectedClipSlotState.fromValues(2, true, RgbLigthState.GRAY_2));

        assertEquals(List.of("resync", "resync"), events);
        assertEquals(2, controller.slotIndex());
        assertTrue(controller.hasContent());
    }

    @Test
    void ensureSelectedClipReportsFailureWhenNoClipContentAvailable() {
        final List<NoteClipAvailability.Failure> failures = new ArrayList<>();
        final ChordStepClipController controller = new ChordStepClipController(
                () -> true,
                () -> false,
                () -> {},
                failures::add);
        controller.refresh(SelectedClipSlotState.fromValues(0, false, RgbLigthState.GRAY_2));

        assertFalse(controller.ensureSelectedClip());
        assertEquals(1, failures.size());
        assertEquals("No Clip", failures.get(0).title());
    }

    @Test
    void ensureSelectedClipSlotReportsFailureWhenTrackCannotHoldNotes() {
        final AtomicBoolean shown = new AtomicBoolean();
        final ChordStepClipController controller = new ChordStepClipController(
                () -> false,
                () -> false,
                () -> {},
                failure -> shown.set(true));

        assertFalse(controller.ensureSelectedClipSlot());
        assertTrue(shown.get());
    }

    @Test
    void ensureSelectedClipSucceedsWhenLoadedContentCanBackfillEmptySelection() {
        final ChordStepClipController controller = new ChordStepClipController(
                () -> true,
                () -> true,
                () -> {},
                failure -> {});
        controller.refresh(SelectedClipSlotState.fromValues(0, false, RgbLigthState.GRAY_2));

        assertTrue(controller.ensureSelectedClip());
    }
}
