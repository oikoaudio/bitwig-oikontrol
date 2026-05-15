package com.oikoaudio.fire.chordstep;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChordStepFineNudgeControllerTest {
    @Test
    void capturesHeldNudgeTargetsAndCompletesThroughAction() {
        final ChordStepFineNudgeState<Event> state = new ChordStepFineNudgeState<>();
        final CapturedAction action = new CapturedAction();
        final ChordStepFineNudgeController<Event> controller = new ChordStepFineNudgeController<>(
                state,
                step -> new Event(List.of(new Note(60 + step, step * 16))),
                action::nudge);

        controller.beginHeldNudge(-1, Set.of(2, 4));

        assertEquals(Set.of(2, 4), state.pendingTargetStepsSnapshot());
        assertEquals(Map.of(62, 32), state.pendingEventsSnapshot().get(2).starts());

        controller.completePendingNudge();

        assertEquals(-1, action.direction);
        assertEquals(Set.of(2, 4), action.targetSteps);
        assertFalse(state.isPendingFineMove());
    }

    private record Event(List<Note> notes) {
        private Map<Integer, Integer> starts() {
            return notes.stream()
                    .collect(java.util.stream.Collectors.toMap(Note::midiNote, Note::fineStart));
        }
    }

    private record Note(int midiNote, int fineStart) {
    }

    private static final class CapturedAction {
        private int direction;
        private Set<Integer> targetSteps = Set.of();

        private void nudge(final int direction, final Set<Integer> targetSteps, final Map<Integer, Event> events) {
            this.direction = direction;
            this.targetSteps = targetSteps;
        }
    }
}
