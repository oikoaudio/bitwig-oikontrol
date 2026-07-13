package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteStep;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies baked variation to the complete set of notes observed for a clip. */
public final class ObservedNoteVariationAdapter {
    private final int observedFineStepCapacity;
    private final Map<NoteVariationEvent.Id, NoteStep> notesById = new LinkedHashMap<>();

    public ObservedNoteVariationAdapter(final int observedFineStepCapacity) {
        this.observedFineStepCapacity = observedFineStepCapacity;
    }

    public void handleObservedNote(final NoteStep note) {
        final NoteVariationEvent.Id id =
                new NoteVariationEvent.Id(note.channel(), note.x(), note.y());
        if (note.state() == NoteStep.State.Empty) {
            notesById.remove(id);
            return;
        }
        notesById.put(id, note);
    }

    public void clear() {
        notesById.clear();
    }

    public Result apply(
            final NoteVariationParameter parameter,
            final double defaultValue,
            final double amount,
            final long seed,
            final int loopFineSteps) {
        if (loopFineSteps > observedFineStepCapacity) {
            return new Result(Status.TOO_LARGE, 0);
        }
        final List<NoteVariationEvent> events =
                notesById.entrySet().stream()
                        .filter(entry -> entry.getValue().state() == NoteStep.State.NoteOn)
                        .filter(entry -> entry.getKey().step() >= 0)
                        .filter(entry -> entry.getKey().step() < loopFineSteps)
                        .map(entry -> new NoteVariationEvent(entry.getKey(), entry.getKey().step()))
                        .toList();
        if (events.isEmpty()) {
            return new Result(Status.EMPTY, 0);
        }
        final NoteVariationPlan plan =
                ScatterNoteVariation.plan(events, parameter, defaultValue, amount, seed);
        for (final NoteVariationAssignment assignment : plan.assignments()) {
            applyValue(notesById.get(assignment.eventId()), parameter, assignment.value());
        }
        return new Result(amount <= 0.0 ? Status.RESET : Status.APPLIED, events.size());
    }

    private void applyValue(
            final NoteStep note, final NoteVariationParameter parameter, final double value) {
        switch (parameter) {
            case VELOCITY -> note.setVelocity(value);
            case PRESSURE -> note.setPressure(value);
            case TIMBRE -> note.setTimbre(value);
            case PITCH -> note.setTranspose(value);
            case PAN -> note.setPan(value);
            case GAIN -> note.setGain(value);
            case CHANCE -> {
                note.setChance(value);
                note.setIsChanceEnabled(value < 0.999999);
            }
            case VELOCITY_SPREAD -> note.setVelocitySpread(value);
        }
    }

    public enum Status {
        APPLIED,
        RESET,
        EMPTY,
        TOO_LARGE
    }

    public record Result(Status status, int noteCount) {}
}
