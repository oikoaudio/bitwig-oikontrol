package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepObservationControllerTest {

    @Test
    void refreshPassClearsCachesAndDelegatesCursorRefresh() {
        final List<String> events = new ArrayList<>();
        final ChordStepClipController clipController = new ChordStepClipController(
                () -> true,
                () -> false,
                () -> events.add("clip-resync"),
                failure -> events.add("failure:" + failure.title()));
        clipController.refresh(SelectedClipSlotState.fromValues(3, true, RgbLigthState.GRAY_2));

        final ChordStepObservationController controller = new ChordStepObservationController(
                (task, delayTicks) -> {},
                null,
                () -> 5,
                () -> RgbLigthState.GRAY_1,
                clipController,
                () -> events.add("clear"),
                () -> events.add("scroll:note-key"),
                () -> events.add("scroll:observed-key"),
                () -> events.add("scroll:note-step"),
                () -> events.add("scroll:observed-step"),
                (slotBank, selectedClipSlotIndex, refreshSelectedClipState, slotIndexSupplier,
                 scrollNoteClipToKeyStart, scrollObservedClipToKeyStart,
                 scrollNoteClipToCurrentStep, scrollObservedClipToStepStart) -> {
                    events.add("refresh:" + selectedClipSlotIndex + ":" + slotIndexSupplier.getAsInt());
                    scrollNoteClipToKeyStart.run();
                    scrollObservedClipToKeyStart.run();
                    scrollNoteClipToCurrentStep.run();
                    scrollObservedClipToStepStart.run();
                },
                (slotBank, defaultColor) -> SelectedClipSlotState.fromValues(3, true, defaultColor));

        controller.refreshPass();

        assertEquals(List.of(
                "clip-resync",
                "clear",
                "refresh:5:3",
                "scroll:note-key",
                "scroll:observed-key",
                "scroll:note-step",
                "scroll:observed-step"), events);
    }

    @Test
    void queueResyncSchedulesOnlyOnePendingRefresh() {
        final List<ScheduledTask> scheduled = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        final ChordStepClipController clipController = new ChordStepClipController(
                () -> true,
                () -> false,
                () -> events.add("clip-resync"),
                failure -> events.add("failure:" + failure.title()));
        final ChordStepObservationController controller = new ChordStepObservationController(
                (task, delayTicks) -> scheduled.add(new ScheduledTask(task, delayTicks)),
                null,
                () -> 0,
                () -> RgbLigthState.GRAY_1,
                clipController,
                () -> events.add("clear"),
                () -> {},
                () -> {},
                () -> {},
                () -> {},
                (slotBank, selectedClipSlotIndex, refreshSelectedClipState, slotIndexSupplier,
                 scrollNoteClipToKeyStart, scrollObservedClipToKeyStart,
                 scrollNoteClipToCurrentStep, scrollObservedClipToStepStart) -> events.add("refresh-pass"),
                (slotBank, defaultColor) -> SelectedClipSlotState.fromValues(0, false, defaultColor));

        controller.queueResync();
        controller.queueResync();

        assertTrue(controller.isResyncQueued());
        assertEquals(List.of(0), scheduled.stream().map(ScheduledTask::delayTicks).toList());

        scheduled.get(0).task().run();

        assertFalse(controller.isResyncQueued());
        assertEquals(List.of("refresh-pass"), events.subList(events.size() - 1, events.size()));
        assertEquals(List.of(0, 1, 6, 18), scheduled.stream().map(ScheduledTask::delayTicks).toList());
    }

    private record ScheduledTask(Runnable task, int delayTicks) {
    }
}
