package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.Clip;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChordStepFineNudgeWriterTest {
    @Test
    void rewritesHeldEventAndTracksInFlightWindow() {
        final Clip clip = mock(Clip.class);
        final ChordStepEventIndex index = index();
        final ChordStepFineNudgeState<ChordStepEventIndex.Event> state = new ChordStepFineNudgeState<>();
        final List<Set<Integer>> modifiedSteps = new ArrayList<>();
        final AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        final AtomicInteger refreshCount = new AtomicInteger();
        final ChordStepFineNudgeWriter writer = new ChordStepFineNudgeWriter(
                clip,
                index,
                state,
                modifiedSteps::add,
                () -> 512,
                global -> global >= 0 && global < 32,
                global -> global,
                (task, delayTicks) -> scheduledTask.set(task),
                refreshCount::incrementAndGet,
                16);
        final ChordStepEventIndex.Event event = new ChordStepEventIndex.Event(2, 2, 32,
                List.of(new ChordStepEventIndex.EventNote(60, 32, 0, 96, 0.25, null)));

        final boolean moved = writer.nudgeHeldNotes(1, Set.of(2), Map.of(2, event));

        assertTrue(moved);
        assertEquals(List.of(Set.of(2)), modifiedSteps);
        verify(clip).clearStep(32, 60);
        verify(clip).setStep(33, 60, 96, 0.25);
        assertEquals(Map.of(60, 33), state.fineStartsForStep(2, true));
        assertEquals(1, refreshCount.get());
        assertTrue(writer.isMoveInFlight());

        scheduledTask.get().run();

        assertFalse(writer.isMoveInFlight());
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
