package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Tracks observed chord-step occupancy, visible notes, and fine-step note starts for the note/chord sequencer.
 */
public final class ChordStepObservedState {
    private final Map<Integer, Set<Integer>> clipNotesByStep = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> fineOccupancyByStep = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> fineNoteStartsByStep = new HashMap<>();

    public void handleObservedStepData(final int fineStep, final int midiNote, final int state,
                                       final int fineStepsPerStep) {
        final int coarseStep = Math.floorDiv(fineStep, fineStepsPerStep);
        final Map<Integer, Set<Integer>> notesAtStep =
                fineOccupancyByStep.computeIfAbsent(coarseStep, ignored -> new HashMap<>());
        final Set<Integer> occupiedFineSteps = notesAtStep.computeIfAbsent(midiNote, ignored -> new HashSet<>());
        if (state == NoteStep.State.Empty.ordinal()) {
            occupiedFineSteps.remove(fineStep);
            if (occupiedFineSteps.isEmpty()) {
                notesAtStep.remove(midiNote);
            }
            if (notesAtStep.isEmpty()) {
                fineOccupancyByStep.remove(coarseStep);
                clipNotesByStep.remove(coarseStep);
            } else {
                clipNotesByStep.put(coarseStep, new HashSet<>(notesAtStep.keySet()));
            }
            final Map<Integer, Integer> noteStarts = fineNoteStartsByStep.get(coarseStep);
            if (noteStarts != null && Integer.valueOf(fineStep).equals(noteStarts.get(midiNote))) {
                noteStarts.remove(midiNote);
                if (noteStarts.isEmpty()) {
                    fineNoteStartsByStep.remove(coarseStep);
                }
            }
            return;
        }

        occupiedFineSteps.add(fineStep);
        clipNotesByStep.computeIfAbsent(coarseStep, ignored -> new HashSet<>()).add(midiNote);
        if (state == NoteStep.State.NoteOn.ordinal()) {
            fineNoteStartsByStep.computeIfAbsent(coarseStep, ignored -> new HashMap<>()).put(midiNote, fineStep);
        }
    }

    public Set<Integer> visibleOccupiedSteps(final IntPredicate isVisibleGlobalStep,
                                             final IntUnaryOperator globalToLocalStep) {
        final Set<Integer> occupiedSteps = new HashSet<>();
        clipNotesByStep.keySet().stream()
                .filter(isVisibleGlobalStep::test)
                .map(globalToLocalStep::applyAsInt)
                .forEach(occupiedSteps::add);
        return occupiedSteps;
    }

    public Set<Integer> visibleStartedSteps(final IntPredicate isVisibleGlobalStep,
                                            final IntUnaryOperator globalToLocalStep) {
        final Set<Integer> startedSteps = new HashSet<>();
        fineNoteStartsByStep.keySet().stream()
                .filter(isVisibleGlobalStep::test)
                .map(globalToLocalStep::applyAsInt)
                .forEach(startedSteps::add);
        return startedSteps;
    }

    public Map<Integer, Integer> noteStartsForStep(final int globalStep) {
        return new HashMap<>(fineNoteStartsByStep.getOrDefault(globalStep, Map.of()));
    }

    public int fineStartFor(final int globalStep, final int midiNote, final int fallbackFineStart) {
        final Map<Integer, Integer> observedStarts = fineNoteStartsByStep.get(globalStep);
        if (observedStarts != null && observedStarts.containsKey(midiNote)) {
            return observedStarts.get(midiNote);
        }
        return fallbackFineStart;
    }

    public boolean isFineStepOccupied(final int fineStep, final int midiNote, final int fineStepsPerStep) {
        final int coarseStep = Math.floorDiv(fineStep, fineStepsPerStep);
        return fineOccupancyByStep
                .getOrDefault(coarseStep, Map.of())
                .getOrDefault(midiNote, Set.of())
                .contains(fineStep);
    }

    public void moveFineStart(final int oldFineStart, final int newFineStart, final int midiNote,
                              final double duration, final int fineStepsPerStep, final double fineStepLength) {
        final int oldGlobalStep = Math.floorDiv(oldFineStart, fineStepsPerStep);
        final int newGlobalStep = Math.floorDiv(newFineStart, fineStepsPerStep);
        final int occupiedFineSteps = Math.max(1, (int) Math.round(duration / fineStepLength));
        for (int offset = 0; offset < occupiedFineSteps; offset++) {
            final int oldFineStep = oldFineStart + offset;
            final int oldStep = Math.floorDiv(oldFineStep, fineStepsPerStep);
            final Map<Integer, Set<Integer>> oldOccupancy = fineOccupancyByStep.get(oldStep);
            if (oldOccupancy != null) {
                final Set<Integer> occupied = oldOccupancy.get(midiNote);
                if (occupied != null) {
                    occupied.remove(oldFineStep);
                    if (occupied.isEmpty()) {
                        oldOccupancy.remove(midiNote);
                    }
                }
                if (oldOccupancy.isEmpty()) {
                    fineOccupancyByStep.remove(oldStep);
                    clipNotesByStep.remove(oldStep);
                } else {
                    clipNotesByStep.put(oldStep, new HashSet<>(oldOccupancy.keySet()));
                }
            }
        }
        final Map<Integer, Integer> oldStarts = fineNoteStartsByStep.get(oldGlobalStep);
        if (oldStarts != null && Integer.valueOf(oldFineStart).equals(oldStarts.get(midiNote))) {
            oldStarts.remove(midiNote);
            if (oldStarts.isEmpty()) {
                fineNoteStartsByStep.remove(oldGlobalStep);
            }
        }
        for (int offset = 0; offset < occupiedFineSteps; offset++) {
            final int newFineStep = newFineStart + offset;
            final int newStep = Math.floorDiv(newFineStep, fineStepsPerStep);
            fineOccupancyByStep
                    .computeIfAbsent(newStep, ignored -> new HashMap<>())
                    .computeIfAbsent(midiNote, ignored -> new HashSet<>())
                    .add(newFineStep);
            clipNotesByStep.computeIfAbsent(newStep, ignored -> new HashSet<>()).add(midiNote);
        }
        fineNoteStartsByStep.computeIfAbsent(newGlobalStep, ignored -> new HashMap<>()).put(midiNote, newFineStart);
    }

    public void invalidateStep(final int globalStep) {
        clipNotesByStep.remove(globalStep);
        fineOccupancyByStep.remove(globalStep);
        fineNoteStartsByStep.remove(globalStep);
    }

    public void clear() {
        clipNotesByStep.clear();
        fineOccupancyByStep.clear();
        fineNoteStartsByStep.clear();
    }

    public boolean hasAnyObservedNotes() {
        return !clipNotesByStep.isEmpty();
    }

    public boolean hasStepContent(final int globalStep) {
        return clipNotesByStep.containsKey(globalStep);
    }

    public boolean hasStepStart(final int globalStep) {
        return fineNoteStartsByStep.containsKey(globalStep);
    }

    public Set<Integer> notesAtStep(final int globalStep) {
        final Set<Integer> notes = clipNotesByStep.get(globalStep);
        if (notes == null || notes.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(notes);
    }
}
