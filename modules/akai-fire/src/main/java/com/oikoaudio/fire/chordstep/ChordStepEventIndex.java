package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Owns chord-step visible note objects, fine-grid observed-note state, and event snapshots.
 */
public final class ChordStepEventIndex {
    private final ChordStepObservedState observedState = new ChordStepObservedState();
    private final Map<Integer, Map<Integer, NoteStep>> noteStepsByPosition = new HashMap<>();
    private final Map<String, NoteStepSnapshot> pendingMovedNotes = new HashMap<>();
    private final IntUnaryOperator localToGlobalStep;
    private final IntUnaryOperator globalToLocalStep;
    private final IntPredicate visibleGlobalStep;
    private final IntUnaryOperator defaultVelocity;
    private final int fineStepsPerStep;
    private final double fineStepLength;
    private final double defaultDuration;

    public ChordStepEventIndex(final IntUnaryOperator localToGlobalStep,
                               final IntUnaryOperator globalToLocalStep,
                               final IntPredicate visibleGlobalStep,
                               final IntUnaryOperator defaultVelocity,
                               final int fineStepsPerStep,
                               final double fineStepLength,
                               final double defaultDuration) {
        this.localToGlobalStep = localToGlobalStep;
        this.globalToLocalStep = globalToLocalStep;
        this.visibleGlobalStep = visibleGlobalStep;
        this.defaultVelocity = defaultVelocity;
        this.fineStepsPerStep = fineStepsPerStep;
        this.fineStepLength = fineStepLength;
        this.defaultDuration = defaultDuration;
    }

    public ChordStepObservedState observedState() {
        return observedState;
    }

    public void handleObservedStepData(final int x, final int y, final int state) {
        observedState.handleObservedStepData(x, y, state, fineStepsPerStep);
    }

    public void handleNoteStepObject(final NoteStep noteStep) {
        final int x = noteStep.x();
        final int y = noteStep.y();
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.computeIfAbsent(x, ignored -> new HashMap<>());
        if (noteStep.state() == NoteStep.State.Empty) {
            notesAtStep.remove(y);
            if (notesAtStep.isEmpty()) {
                noteStepsByPosition.remove(x);
            }
            return;
        }
        notesAtStep.put(y, noteStep);
        final NoteStepSnapshot pending = pendingMovedNotes.remove(moveKey(x, y));
        if (pending != null) {
            pending.applyTo(noteStep);
        }
    }

    public void addPendingMoveSnapshot(final int localStep, final int midiNote, final NoteStep noteStep) {
        pendingMovedNotes.put(moveKey(localStep, midiNote), NoteStepSnapshot.capture(noteStep));
    }

    public void clear() {
        observedState.clear();
        pendingMovedNotes.clear();
    }

    public Map<Integer, NoteStep> noteStepsAt(final int localStep) {
        return noteStepsByPosition.getOrDefault(localStep, Map.of());
    }

    public boolean hasAnyLoadedNoteContent() {
        return observedState.hasAnyObservedNotes()
                || noteStepsByPosition.values().stream()
                .flatMap(notes -> notes.values().stream())
                .anyMatch(note -> note.state() == NoteStep.State.NoteOn);
    }

    public boolean hasVisibleStepContent(final int localStep) {
        final int globalStep = localToGlobalStep.applyAsInt(localStep);
        if (observedState.hasStepContent(globalStep)) {
            return true;
        }
        return noteStepsAt(localStep).values().stream()
                .anyMatch(note -> note.state() == NoteStep.State.NoteOn || note.state() == NoteStep.State.NoteSustain);
    }

    public boolean hasStepStart(final int localStep) {
        final int globalStep = localToGlobalStep.applyAsInt(localStep);
        if (observedState.hasStepStart(globalStep)) {
            return true;
        }
        return noteStepsAt(localStep).values().stream()
                .anyMatch(note -> note.state() == NoteStep.State.NoteOn);
    }

    public Set<Integer> notesAtStep(final int localStep) {
        final Set<Integer> observedNotes = observedState.notesAtStep(localToGlobalStep.applyAsInt(localStep));
        if (observedNotes != null && !observedNotes.isEmpty()) {
            return new HashSet<>(observedNotes);
        }
        return new HashSet<>(noteStepsAt(localStep).keySet());
    }

    public Set<Integer> visibleOccupiedSteps() {
        final Set<Integer> occupiedSteps = new HashSet<>(noteStepsByPosition.keySet());
        occupiedSteps.addAll(observedState.visibleOccupiedSteps(
                visibleGlobalStep,
                globalToLocalStep));
        return occupiedSteps;
    }

    public Set<Integer> visibleStartedSteps() {
        final Set<Integer> startedSteps = observedState.visibleStartedSteps(visibleGlobalStep, globalToLocalStep);
        noteStepsByPosition.entrySet().stream()
                .filter(entry -> entry.getValue().values().stream().anyMatch(note -> note.state() == NoteStep.State.NoteOn))
                .map(Map.Entry::getKey)
                .forEach(startedSteps::add);
        return startedSteps;
    }

    public List<NoteStep> allStartedNotes() {
        return noteStepsByPosition.values().stream()
                .flatMap(notes -> notes.values().stream())
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .toList();
    }

    public List<NoteStep> heldNotes(final Set<Integer> heldSteps) {
        return heldSteps.stream()
                .flatMap(step -> noteStepsAt(step).values().stream())
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .toList();
    }

    public List<NoteStep> selectedStepNotes(final Integer selectedStepIndex) {
        if (selectedStepIndex == null) {
            return List.of();
        }
        return noteStepsAt(selectedStepIndex).values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .toList();
    }

    public Event eventForStep(final int localStep, final Map<Integer, Integer> startOverrides) {
        return eventsForStep(localStep, startOverrides).stream().findFirst().orElse(null);
    }

    public List<Event> eventsForStep(final int localStep, final Map<Integer, Integer> startOverrides) {
        final Map<Integer, NoteStep> visibleNotes = noteStepsByPosition.get(localStep);
        final int globalStep = localToGlobalStep.applyAsInt(localStep);
        final Map<Integer, Integer> noteStarts = new HashMap<>();
        noteStarts.putAll(observedState.noteStartsForStep(globalStep));
        if (!startOverrides.isEmpty()) {
            noteStarts.putAll(startOverrides);
        }
        if (visibleNotes != null) {
            visibleNotes.values().stream()
                    .filter(note -> note.state() == NoteStep.State.NoteOn)
                    .forEach(note -> noteStarts.putIfAbsent(note.y(), fineStartFor(globalStep, note.y(), note.x())));
        }
        if (noteStarts.isEmpty()) {
            return List.of();
        }
        final int anchorFineStart = noteStarts.values().stream()
                .min(Integer::compareTo)
                .orElse(globalStep * fineStepsPerStep);
        final boolean normalizeToSharedAnchor = noteStarts.size() > 1
                && noteStarts.values().stream().distinct().count() == 1;
        final List<EventNote> notes = noteStarts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> {
                    final int midiNote = entry.getKey();
                    final int fineStart = entry.getValue();
                    final int effectiveFineStart = normalizeToSharedAnchor ? anchorFineStart : fineStart;
                    final NoteStep visibleNote = visibleNotes == null ? null : visibleNotes.get(midiNote);
                    final int velocity = visibleNote == null
                            ? defaultVelocity.applyAsInt(127)
                            : (int) Math.round(visibleNote.velocity() * 127);
                    final double duration = visibleNote != null
                            ? visibleNote.duration()
                            : durationForObservedNote(fineStart, midiNote);
                    return new EventNote(
                            midiNote,
                            effectiveFineStart,
                            effectiveFineStart - anchorFineStart,
                            velocity,
                            duration,
                            visibleNote);
                })
                .toList();
        return List.of(new Event(localStep, globalStep, anchorFineStart, notes));
    }

    public int fineStartFor(final int globalStep, final int midiNote, final int fallbackLocalStep) {
        return observedState.fineStartFor(
                globalStep,
                midiNote,
                localToGlobalStep.applyAsInt(fallbackLocalStep) * fineStepsPerStep);
    }

    public void moveFineStart(final int oldFineStart, final int newFineStart, final int midiNote,
                              final double duration) {
        observedState.moveFineStart(oldFineStart, newFineStart, midiNote, duration, fineStepsPerStep, fineStepLength);
    }

    public List<EventNoteMove> createNoteMovesForEvent(final Event event,
                                                       final int targetAnchorFineStart,
                                                       final int loopFineSteps) {
        return event.notes().stream()
                .map(note -> new EventNoteMove(
                        event.localStep(),
                        note,
                        Math.floorMod(targetAnchorFineStart + note.startOffset(), loopFineSteps)))
                .filter(move -> move.sourceFineStart() != move.targetFineStart())
                .toList();
    }

    public Event moveEvent(final Event event, final int targetAnchorFineStart, final int loopFineSteps) {
        final List<EventNote> movedNotes = event.notes().stream()
                .map(note -> new EventNote(
                        note.midiNote(),
                        Math.floorMod(targetAnchorFineStart + note.startOffset(), loopFineSteps),
                        note.startOffset(),
                        note.velocity(),
                        note.duration(),
                        note.visibleStep()))
                .toList();
        return new Event(
                event.localStep(),
                Math.floorDiv(targetAnchorFineStart, fineStepsPerStep),
                targetAnchorFineStart,
                movedNotes);
    }

    private double durationForObservedNote(final int fineStart, final int midiNote) {
        int occupiedFineSteps = 0;
        int currentFineStep = fineStart;
        while (observedState.isFineStepOccupied(currentFineStep, midiNote, fineStepsPerStep)) {
            occupiedFineSteps++;
            currentFineStep++;
        }
        if (occupiedFineSteps == 0) {
            return defaultDuration;
        }
        return occupiedFineSteps * fineStepLength;
    }

    private static String moveKey(final int x, final int y) {
        return x + ":" + y;
    }

    public record Event(int localStep, int globalStep, int anchorFineStart, List<EventNote> notes) {
    }

    public record EventNote(int midiNote, int fineStart, int startOffset, int velocity, double duration,
                            NoteStep visibleStep) {
    }

    public record EventNoteMove(int localStep, EventNote note, int targetFineStart) {
        public int sourceFineStart() {
            return note.fineStart();
        }

        public int midiNote() {
            return note.midiNote();
        }

        public int velocity() {
            return note.velocity();
        }

        public double duration() {
            return note.duration();
        }

        public NoteStep visibleStep() {
            return note.visibleStep();
        }
    }

    private record NoteStepSnapshot(double chance, double pressure, double timbre, double velocitySpread,
                                    int repeatCount, double repeatCurve, double repeatVelocityCurve,
                                    double repeatVelocityEnd, double pan, int recurrenceLength, int recurrenceMask,
                                    NoteOccurrence occurrence) {
        static NoteStepSnapshot capture(final NoteStep step) {
            return new NoteStepSnapshot(
                    step.chance(),
                    step.pressure(),
                    step.timbre(),
                    step.velocitySpread(),
                    step.repeatCount(),
                    step.repeatCurve(),
                    step.repeatVelocityCurve(),
                    step.repeatVelocityEnd(),
                    step.pan(),
                    step.recurrenceLength(),
                    step.recurrenceMask(),
                    step.occurrence());
        }

        void applyTo(final NoteStep dest) {
            dest.setChance(chance);
            dest.setPressure(pressure);
            dest.setTimbre(timbre);
            dest.setVelocitySpread(velocitySpread);
            dest.setRepeatCount(repeatCount);
            dest.setRepeatCurve(repeatCurve);
            dest.setRepeatVelocityCurve(repeatVelocityCurve);
            dest.setRepeatVelocityEnd(repeatVelocityEnd);
            dest.setPan(pan);
            dest.setRecurrence(recurrenceLength, recurrenceMask);
            dest.setOccurrence(occurrence);
        }
    }
}
