package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.NoteStep;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepObservedStateTest {
    private static final int FINE_STEPS_PER_STEP = 16;
    private static final double FINE_STEP_LENGTH = 0.25 / FINE_STEPS_PER_STEP;

    @Test
    void noteOnTracksStepContentAndStart() {
        final ChordStepObservedState state = new ChordStepObservedState();

        state.handleObservedStepData(32, 60, NoteStep.State.NoteOn.ordinal(), FINE_STEPS_PER_STEP);

        assertTrue(state.hasStepContent(2));
        assertTrue(state.hasStepStart(2));
        assertEquals(Set.of(60), state.notesAtStep(2));
        assertEquals(32, state.fineStartFor(2, 60, -1));
        assertTrue(state.isFineStepOccupied(32, 60, FINE_STEPS_PER_STEP));
    }

    @Test
    void emptyStateRemovesTrackedContentAndStart() {
        final ChordStepObservedState state = new ChordStepObservedState();

        state.handleObservedStepData(32, 60, NoteStep.State.NoteOn.ordinal(), FINE_STEPS_PER_STEP);
        state.handleObservedStepData(32, 60, NoteStep.State.Empty.ordinal(), FINE_STEPS_PER_STEP);

        assertFalse(state.hasStepContent(2));
        assertFalse(state.hasStepStart(2));
        assertEquals(Set.of(), state.notesAtStep(2));
        assertEquals(-1, state.fineStartFor(2, 60, -1));
        assertFalse(state.isFineStepOccupied(32, 60, FINE_STEPS_PER_STEP));
    }

    @Test
    void moveFineStartUpdatesOccupancyAcrossOldAndNewRanges() {
        final ChordStepObservedState state = new ChordStepObservedState();

        state.handleObservedStepData(32, 60, NoteStep.State.NoteOn.ordinal(), FINE_STEPS_PER_STEP);
        state.handleObservedStepData(33, 60, NoteStep.State.NoteSustain.ordinal(), FINE_STEPS_PER_STEP);
        state.handleObservedStepData(34, 60, NoteStep.State.NoteSustain.ordinal(), FINE_STEPS_PER_STEP);

        state.moveFineStart(32, 48, 60, 3 * FINE_STEP_LENGTH, FINE_STEPS_PER_STEP, FINE_STEP_LENGTH);

        assertFalse(state.hasStepContent(2));
        assertFalse(state.hasStepStart(2));
        assertTrue(state.hasStepContent(3));
        assertTrue(state.hasStepStart(3));
        assertEquals(48, state.fineStartFor(3, 60, -1));
        assertFalse(state.isFineStepOccupied(32, 60, FINE_STEPS_PER_STEP));
        assertTrue(state.isFineStepOccupied(48, 60, FINE_STEPS_PER_STEP));
        assertTrue(state.isFineStepOccupied(49, 60, FINE_STEPS_PER_STEP));
        assertTrue(state.isFineStepOccupied(50, 60, FINE_STEPS_PER_STEP));
    }

    @Test
    void visibleStepQueriesMapGlobalStepsToLocalSteps() {
        final ChordStepObservedState state = new ChordStepObservedState();

        state.handleObservedStepData(16, 60, NoteStep.State.NoteOn.ordinal(), FINE_STEPS_PER_STEP);
        state.handleObservedStepData(48, 64, NoteStep.State.NoteOn.ordinal(), FINE_STEPS_PER_STEP);

        assertEquals(Set.of(1, 3),
                state.visibleOccupiedSteps(global -> global >= 1 && global <= 3, global -> global));
        assertEquals(Set.of(1, 3),
                state.visibleStartedSteps(global -> global >= 1 && global <= 3, global -> global));
    }

    @Test
    void invalidateStepDropsOnlyThatStep() {
        final ChordStepObservedState state = new ChordStepObservedState();

        state.handleObservedStepData(16, 60, NoteStep.State.NoteOn.ordinal(), FINE_STEPS_PER_STEP);
        state.handleObservedStepData(32, 64, NoteStep.State.NoteOn.ordinal(), FINE_STEPS_PER_STEP);

        state.invalidateStep(1);

        assertFalse(state.hasStepContent(1));
        assertTrue(state.hasStepContent(2));
        assertEquals(Set.of(64), state.notesAtStep(2));
    }
}
