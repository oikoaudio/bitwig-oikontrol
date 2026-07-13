package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScatterNoteVariationTest {

    @Test
    void zeroAmountAssignsTheCurrentDefaultToEveryEvent() {
        final List<NoteVariationEvent> events =
                List.of(event(0, 60, 0), event(4, 64, 4), event(8, 67, 8));

        final NoteVariationPlan plan =
                ScatterNoteVariation.plan(events, NoteVariationParameter.TIMBRE, 0.25, 0.0, 42L);

        assertEquals(3, plan.assignments().size());
        plan.assignments().forEach(assignment -> assertEquals(0.25, assignment.value()));
    }

    @Test
    void sameSeedAndUnorderedInputProduceTheSameAssignments() {
        final List<NoteVariationEvent> ordered =
                List.of(event(0, 60, 0), event(4, 64, 4), event(8, 67, 8));
        final List<NoteVariationEvent> shuffled =
                List.of(event(8, 67, 8), event(0, 60, 0), event(4, 64, 4));

        final NoteVariationPlan first =
                ScatterNoteVariation.plan(ordered, NoteVariationParameter.PRESSURE, 0.5, 0.7, 91L);
        final NoteVariationPlan second =
                ScatterNoteVariation.plan(shuffled, NoteVariationParameter.PRESSURE, 0.5, 0.7, 91L);

        assertEquals(first.assignmentsByEvent(), second.assignmentsByEvent());
    }

    @Test
    void simultaneousChordVoicesReceiveTheSamePrimaryValue() {
        final NoteVariationEvent root = event(4, 60, 4);
        final NoteVariationEvent third = event(4, 64, 4);
        final NoteVariationEvent fifth = event(4, 67, 4);

        final NoteVariationPlan plan =
                ScatterNoteVariation.plan(
                        List.of(root, third, fifth),
                        NoteVariationParameter.VELOCITY,
                        0.75,
                        0.8,
                        19L);

        assertEquals(plan.valueFor(root.id()), plan.valueFor(third.id()));
        assertEquals(plan.valueFor(root.id()), plan.valueFor(fifth.id()));
    }

    @Test
    void differentSeedsNormallyProduceDifferentAssignments() {
        final List<NoteVariationEvent> events =
                List.of(event(0, 60, 0), event(4, 64, 4), event(8, 67, 8));

        final NoteVariationPlan first =
                ScatterNoteVariation.plan(events, NoteVariationParameter.TIMBRE, 0.0, 1.0, 1L);
        final NoteVariationPlan second =
                ScatterNoteVariation.plan(events, NoteVariationParameter.TIMBRE, 0.0, 1.0, 2L);

        assertNotEquals(first.assignmentsByEvent(), second.assignmentsByEvent());
    }

    @Test
    void pitchUsesTwelveSemitoneMusicalEnvelope() {
        final List<NoteVariationEvent> events =
                java.util.stream.IntStream.range(0, 128)
                        .mapToObj(index -> event(index, 60, index))
                        .toList();

        final NoteVariationPlan plan =
                ScatterNoteVariation.plan(events, NoteVariationParameter.PITCH, 5.0, 1.0, 71L);

        plan.assignments()
                .forEach(
                        assignment -> {
                            assertTrue(assignment.value() >= -7.0);
                            assertTrue(assignment.value() <= 17.0);
                        });
    }

    @Test
    void eligibleExistingNoteTargetsMapToVariationParameters() {
        assertEquals(
                NoteVariationParameter.VELOCITY,
                NoteVariationParameter.from(NoteStepAccess.VELOCITY).orElseThrow());
        assertEquals(
                NoteVariationParameter.PRESSURE,
                NoteVariationParameter.from(NoteStepAccess.PRESSURE).orElseThrow());
        assertEquals(
                NoteVariationParameter.TIMBRE,
                NoteVariationParameter.from(NoteStepAccess.TIMBRE).orElseThrow());
        assertEquals(
                NoteVariationParameter.PITCH,
                NoteVariationParameter.from(NoteStepAccess.PITCH).orElseThrow());
        assertEquals(
                NoteVariationParameter.CHANCE,
                NoteVariationParameter.from(NoteStepAccess.CHANCE).orElseThrow());
        assertEquals(
                NoteVariationParameter.VELOCITY_SPREAD,
                NoteVariationParameter.from(NoteStepAccess.VELOCITY_SPREAD).orElseThrow());
        assertTrue(NoteVariationParameter.from(NoteStepAccess.DURATION).isEmpty());
        assertTrue(NoteVariationParameter.from(NoteStepAccess.REPEATS).isEmpty());
    }

    private static NoteVariationEvent event(final int step, final int pitch, final long onsetKey) {
        return new NoteVariationEvent(new NoteVariationEvent.Id(0, step, pitch), onsetKey);
    }
}
