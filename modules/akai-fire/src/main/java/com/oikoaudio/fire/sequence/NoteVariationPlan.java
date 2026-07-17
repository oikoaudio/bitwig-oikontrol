package com.oikoaudio.fire.sequence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete immutable assignments produced before a mode performs any note writes. */
public final class NoteVariationPlan {
    private final NoteVariationParameter parameter;
    private final List<NoteVariationAssignment> assignments;
    private final Map<NoteVariationEvent.Id, Double> assignmentsByEvent;

    NoteVariationPlan(
            final NoteVariationParameter parameter,
            final List<NoteVariationAssignment> assignments) {
        this.parameter = parameter;
        this.assignments = List.copyOf(assignments);
        final Map<NoteVariationEvent.Id, Double> byEvent = new LinkedHashMap<>();
        for (final NoteVariationAssignment assignment : assignments) {
            if (byEvent.put(assignment.eventId(), assignment.value()) != null) {
                throw new IllegalArgumentException(
                        "Duplicate note variation event " + assignment.eventId());
            }
        }
        assignmentsByEvent = Map.copyOf(byEvent);
    }

    public NoteVariationParameter parameter() {
        return parameter;
    }

    public List<NoteVariationAssignment> assignments() {
        return assignments;
    }

    public Map<NoteVariationEvent.Id, Double> assignmentsByEvent() {
        return assignmentsByEvent;
    }

    public double valueFor(final NoteVariationEvent.Id eventId) {
        final Double value = assignmentsByEvent.get(eventId);
        if (value == null) {
            throw new IllegalArgumentException("No assignment for " + eventId);
        }
        return value;
    }
}
