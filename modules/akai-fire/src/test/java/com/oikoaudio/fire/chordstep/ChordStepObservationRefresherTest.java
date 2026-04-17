package com.oikoaudio.fire.chordstep;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepObservationRefresherTest {

    @Test
    void queueResyncSchedulesOnlyOnePendingRefresh() {
        final List<ScheduledTask> scheduled = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        final ChordStepObservationRefresher refresher = new ChordStepObservationRefresher(
                (task, delayTicks) -> scheduled.add(new ScheduledTask(task, delayTicks)),
                () -> events.add("selected"),
                () -> events.add("pass"));

        refresher.queueResync();
        refresher.queueResync();

        assertTrue(refresher.isResyncQueued());
        assertEquals(List.of(0), scheduled.stream().map(ScheduledTask::delayTicks).toList());

        scheduled.get(0).task().run();

        assertFalse(refresher.isResyncQueued());
        assertEquals(List.of("selected", "pass"), events.subList(0, 2));
        assertEquals(List.of(0, 1, 6, 18), scheduled.stream().map(ScheduledTask::delayTicks).toList());
    }

    @Test
    void refreshRunsImmediatePassAndSchedulesFollowUps() {
        final List<ScheduledTask> scheduled = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        final ChordStepObservationRefresher refresher = new ChordStepObservationRefresher(
                (task, delayTicks) -> scheduled.add(new ScheduledTask(task, delayTicks)),
                () -> events.add("selected"),
                () -> events.add("pass"));

        refresher.refresh();

        assertEquals(List.of("selected", "pass"), events);
        assertEquals(List.of(1, 6, 18), scheduled.stream().map(ScheduledTask::delayTicks).toList());

        scheduled.forEach(task -> task.task().run());

        assertEquals(List.of("selected", "pass", "pass", "pass", "pass"), events);
    }

    private record ScheduledTask(Runnable task, int delayTicks) {
    }
}
