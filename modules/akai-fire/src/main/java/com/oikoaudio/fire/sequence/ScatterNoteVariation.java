package com.oikoaudio.fire.sequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Seeded triangular Baked Note Variation with coherent same-onset values. */
public final class ScatterNoteVariation {
    private ScatterNoteVariation() {}

    public static NoteVariationPlan plan(
            final List<NoteVariationEvent> events,
            final NoteVariationParameter parameter,
            final double defaultValue,
            final double amount,
            final long seed) {
        final Map<Long, List<NoteVariationEvent>> eventsByOnset = new TreeMap<>();
        for (final NoteVariationEvent event : events) {
            eventsByOnset
                    .computeIfAbsent(event.onsetKey(), ignored -> new ArrayList<>())
                    .add(event);
        }
        eventsByOnset
                .values()
                .forEach(group -> group.sort(Comparator.comparing(NoteVariationEvent::id)));

        final NoteVariationParameter.Bounds bounds = parameter.bounds(defaultValue, amount);
        final Random random = new Random(seed);
        final List<NoteVariationAssignment> assignments = new ArrayList<>(events.size());
        for (final List<NoteVariationEvent> onsetGroup : eventsByOnset.values()) {
            final double normalizedSample =
                    amount <= 0.0 ? 0.5 : (random.nextDouble() + random.nextDouble()) * 0.5;
            final double value = parameter.clamp(valueAt(bounds, normalizedSample));
            for (final NoteVariationEvent event : onsetGroup) {
                assignments.add(new NoteVariationAssignment(event.id(), value));
            }
        }
        assignments.sort(Comparator.comparing(NoteVariationAssignment::eventId));
        return new NoteVariationPlan(parameter, assignments);
    }

    private static double valueAt(
            final NoteVariationParameter.Bounds bounds, final double normalizedSample) {
        if (normalizedSample <= 0.5) {
            return bounds.lower() + (bounds.center() - bounds.lower()) * normalizedSample * 2.0;
        }
        return bounds.center()
                + (bounds.upper() - bounds.center()) * (normalizedSample - 0.5) * 2.0;
    }
}
