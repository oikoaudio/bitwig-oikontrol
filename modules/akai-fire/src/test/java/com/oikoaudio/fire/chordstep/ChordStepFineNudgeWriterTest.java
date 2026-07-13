package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteStep;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChordStepFineNudgeWriterTest {
    @Test
    void rewritesHeldEventAndTracksInFlightWindow() {
        final Clip clip = mock(Clip.class);
        final ChordStepEventIndex index = index();
        final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session =
                new ChordStepFineNudgeSession<>(step -> null, (amount, steps, events) -> {});
        final List<Set<Integer>> modifiedSteps = new ArrayList<>();
        final AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        final AtomicInteger refreshCount = new AtomicInteger();
        final ChordStepFineNudgeWriter writer =
                new ChordStepFineNudgeWriter(
                        clip,
                        index,
                        session,
                        modifiedSteps::add,
                        () -> 512,
                        global -> global >= 0 && global < 32,
                        global -> global,
                        (task, delayTicks) -> scheduledTask.set(task),
                        refreshCount::incrementAndGet,
                        16);
        final ChordStepEventIndex.Event event =
                new ChordStepEventIndex.Event(
                        2,
                        2,
                        32,
                        List.of(new ChordStepEventIndex.EventNote(60, 32, 0, 96, 0.25, null)));

        final boolean moved = writer.nudgeHeldNotes(1, Set.of(2), Map.of(2, event));

        assertTrue(moved);
        assertEquals(List.of(Set.of(2)), modifiedSteps);
        verify(clip).clearStep(32, 60);
        verify(clip).setStep(33, 60, 96, 0.25);
        assertEquals(Map.of(60, 33), session.fineStartsForStep(2, true));
        assertEquals(1, refreshCount.get());
        assertTrue(writer.isMoveInFlight());

        scheduledTask.get().run();

        assertFalse(writer.isMoveInFlight());
    }

    @Test
    void olderDelayedTaskCannotClearNewerMoveInFlightWindow() {
        final Clip clip = mock(Clip.class);
        final ChordStepEventIndex index = index();
        final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session =
                new ChordStepFineNudgeSession<>(step -> null, (amount, steps, events) -> {});
        final List<Runnable> scheduledTasks = new ArrayList<>();
        final ChordStepFineNudgeWriter writer =
                new ChordStepFineNudgeWriter(
                        clip,
                        index,
                        session,
                        steps -> {},
                        () -> 512,
                        global -> global >= 0 && global < 32,
                        global -> global,
                        (task, delayTicks) -> scheduledTasks.add(task),
                        () -> {},
                        16);
        final ChordStepEventIndex.Event event =
                new ChordStepEventIndex.Event(
                        2,
                        2,
                        32,
                        List.of(new ChordStepEventIndex.EventNote(60, 32, 0, 96, 0.25, null)));

        writer.nudgeHeldNotes(1, Set.of(2), Map.of(2, event));
        writer.nudgeHeldNotes(1, Set.of(2), Map.of(2, event));

        scheduledTasks.get(0).run();
        assertTrue(writer.isMoveInFlight());

        scheduledTasks.get(1).run();
        assertFalse(writer.isMoveInFlight());
    }

    @Test
    void transfersOwnershipAtTheMidpointWithoutChangingDuration() {
        final Clip clip = mock(Clip.class);
        final ChordStepEventIndex index = index();
        index.handleObservedStepData(39, 60, NoteStep.State.NoteOn.ordinal());
        final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session =
                new ChordStepFineNudgeSession<>(step -> null, (amount, steps, events) -> {});
        final ChordStepFineNudgeWriter writer =
                new ChordStepFineNudgeWriter(
                        clip,
                        index,
                        session,
                        steps -> {},
                        () -> 512,
                        global -> global >= 0 && global < 32,
                        global -> global,
                        (task, delayTicks) -> {},
                        () -> {},
                        16);
        final ChordStepEventIndex.Event event =
                new ChordStepEventIndex.Event(
                        2,
                        2,
                        39,
                        List.of(new ChordStepEventIndex.EventNote(60, 39, 0, 96, 0.75, null)));

        writer.nudgeHeldNotes(1, Set.of(2), Map.of(2, event));

        assertFalse(index.hasStepStart(2));
        assertTrue(index.hasStepStart(3));
        verify(clip).setStep(40, 60, 96, 0.75);
    }

    private static ChordStepEventIndex index() {
        return new ChordStepEventIndex(
                local -> local,
                global -> global,
                global -> global >= 0 && global < 32,
                ignored -> 96,
                16,
                0.25 / 16.0,
                0.25);
    }
}
