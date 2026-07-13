package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChordStepFineNudgeSessionTest {
    @Test
    void capturesTargetsAndCompletesPendingMove() {
        final AtomicReference<Move<String>> captured = new AtomicReference<>();
        final ChordStepFineNudgeSession<String> session =
                new ChordStepFineNudgeSession<>(
                        step -> step == 2 ? "event" : null,
                        (direction, targetSteps, events) ->
                                captured.set(new Move<>(direction, targetSteps, events)));

        session.beginHeldNudge(-1, Set.of(2, 4));

        assertTrue(session.completePendingNudge());
        assertEquals(new Move<>(-1, Set.of(2, 4), Map.of(2, "event")), captured.get());
        assertFalse(session.completePendingNudge());
    }

    @Test
    void lengthAdjustmentAndFineMoveAreMutuallyExclusive() {
        final ChordStepFineNudgeSession<String> session = session();

        session.beginHeldNudge(1, Set.of(2));
        session.setPendingLengthAdjust(true);

        assertTrue(session.isPendingLengthAdjust());
        assertFalse(session.completePendingNudge());

        session.beginHeldNudge(-1, Set.of(3));

        assertFalse(session.isPendingLengthAdjust());
        assertTrue(session.completePendingNudge());
    }

    @Test
    void invalidationAndHeldCleanupRemoveRememberedState() {
        final ChordStepFineNudgeSession<String> session = session();
        session.putHeldEvent(2, "held");
        session.putHeldFineStart(2, 60, 32);

        session.invalidateStep(2);

        assertNull(session.heldEvent(2));
        assertNull(session.fineStartsForStep(2, true));
    }

    private static ChordStepFineNudgeSession<String> session() {
        return new ChordStepFineNudgeSession<>(
                step -> "event-" + step, (amount, steps, events) -> {});
    }

    private record Move<E>(int direction, Set<Integer> targetSteps, Map<Integer, E> events) {}
}
