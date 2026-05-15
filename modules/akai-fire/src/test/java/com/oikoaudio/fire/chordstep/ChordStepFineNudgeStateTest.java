package com.oikoaudio.fire.chordstep;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepFineNudgeStateTest {
    @Test
    void tracksHeldSessionState() {
        final ChordStepFineNudgeState<String> state = new ChordStepFineNudgeState<>();

        state.putHeldEvent(2, "held");
        state.putHeldFineStart(2, 60, 32);

        assertEquals("held", state.heldEvent(2));
        assertEquals(Map.of(60, 32), state.fineStartsForStep(2, true));

        state.clearHeld();

        assertNull(state.heldEvent(2));
    }

    @Test
    void invalidatingStepClearsAllSessionCopiesForThatStep() {
        final ChordStepFineNudgeState<String> state = new ChordStepFineNudgeState<>();
        state.putHeldEvent(4, "held");
        state.beginPending(1, true);
        state.addPendingTargetSteps(Set.of(4));
        state.putPendingEvent(4, "pending");

        state.invalidateStep(4);

        assertNull(state.heldEvent(4));
        assertFalse(state.pendingTargetStepsSnapshot().contains(4));
        assertFalse(state.pendingEventsSnapshot().containsKey(4));
    }

    @Test
    void pendingStateSnapshotsAreDefensiveCopies() {
        final ChordStepFineNudgeState<String> state = new ChordStepFineNudgeState<>();
        state.beginPending(-1, true);
        state.addPendingTargetSteps(Set.of(1, 2));
        state.putPendingEvent(1, "a");

        final Set<Integer> targets = state.pendingTargetStepsSnapshot();
        final Map<Integer, String> events = state.pendingEventsSnapshot();

        assertEquals(-1, state.pendingDirection());
        assertTrue(state.isPendingFineMove());
        assertEquals(Set.of(1, 2), targets);
        assertEquals(Map.of(1, "a"), events);

        state.clearPending();

        assertFalse(state.isPendingFineMove());
        assertEquals(0, state.pendingDirection());
        assertTrue(state.pendingTargetStepsSnapshot().isEmpty());
    }
}
