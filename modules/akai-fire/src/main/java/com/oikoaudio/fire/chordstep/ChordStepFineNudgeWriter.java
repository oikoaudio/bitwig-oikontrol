package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.Clip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * Owns held-step fine-nudge clip rewrites and the short in-flight suppression window after writes.
 */
public final class ChordStepFineNudgeWriter {
    private final Clip observedClip;
    private final ChordStepEventIndex eventIndex;
    private final ChordStepFineNudgeState<ChordStepEventIndex.Event> state;
    private final Consumer<Set<Integer>> markModifiedSteps;
    private final IntSupplier loopFineSteps;
    private final IntPredicate visibleGlobalStep;
    private final IntUnaryOperator globalToLocalStep;
    private final BiConsumer<Runnable, Integer> scheduleTask;
    private final Runnable refreshObservation;
    private final int fineStepsPerStep;

    private boolean moveInFlight = false;
    private int moveGeneration = 0;

    public ChordStepFineNudgeWriter(final Clip observedClip,
                                    final ChordStepEventIndex eventIndex,
                                    final ChordStepFineNudgeState<ChordStepEventIndex.Event> state,
                                    final Consumer<Set<Integer>> markModifiedSteps,
                                    final IntSupplier loopFineSteps,
                                    final IntPredicate visibleGlobalStep,
                                    final IntUnaryOperator globalToLocalStep,
                                    final BiConsumer<Runnable, Integer> scheduleTask,
                                    final Runnable refreshObservation,
                                    final int fineStepsPerStep) {
        this.observedClip = observedClip;
        this.eventIndex = eventIndex;
        this.state = state;
        this.markModifiedSteps = markModifiedSteps;
        this.loopFineSteps = loopFineSteps;
        this.visibleGlobalStep = visibleGlobalStep;
        this.globalToLocalStep = globalToLocalStep;
        this.scheduleTask = scheduleTask;
        this.refreshObservation = refreshObservation;
        this.fineStepsPerStep = fineStepsPerStep;
    }

    public boolean nudgeHeldNotes(final int amount,
                                  final Set<Integer> targetSteps,
                                  final Map<Integer, ChordStepEventIndex.Event> chordEventSnapshot) {
        if (targetSteps.isEmpty() || chordEventSnapshot.isEmpty()) {
            return false;
        }
        markModifiedSteps.accept(targetSteps);
        final List<ChordStepEventIndex.Event> eventsToNudge = targetSteps.stream()
                .map(chordEventSnapshot::get)
                .filter(java.util.Objects::nonNull)
                .sorted(amount > 0
                        ? Comparator.comparingInt(ChordStepEventIndex.Event::anchorFineStart).reversed()
                        : Comparator.comparingInt(ChordStepEventIndex.Event::anchorFineStart))
                .toList();
        if (eventsToNudge.isEmpty()) {
            return false;
        }
        state.beginHeldNudge(targetSteps);
        final int loopFineStepCount = loopFineSteps.getAsInt();
        final List<ChordStepEventIndex.EventNoteMove> noteMoves = new ArrayList<>();
        final Map<Integer, ChordStepEventIndex.Event> movedEvents = new HashMap<>();
        for (final ChordStepEventIndex.Event event : eventsToNudge) {
            final int targetAnchorFineStart = Math.floorMod(event.anchorFineStart() + amount, loopFineStepCount);
            noteMoves.addAll(eventIndex.createNoteMovesForEvent(event, targetAnchorFineStart, loopFineStepCount));
            movedEvents.put(event.localStep(), eventIndex.moveEvent(event, targetAnchorFineStart, loopFineStepCount));
        }
        if (noteMoves.isEmpty()) {
            return false;
        }
        rewriteEventMoves(noteMoves);
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            state.putHeldFineStart(move.localStep(), move.midiNote(), move.targetFineStart());
        }
        movedEvents.forEach(state::putHeldEvent);
        return true;
    }

    public boolean isMoveInFlight() {
        return moveInFlight;
    }

    private void rewriteEventMoves(final List<ChordStepEventIndex.EventNoteMove> noteMoves) {
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            final int targetGlobalStep = Math.floorDiv(move.targetFineStart(), fineStepsPerStep);
            if (move.visibleStep() != null && visibleGlobalStep.test(targetGlobalStep)) {
                eventIndex.addPendingMoveSnapshot(globalToLocalStep.applyAsInt(targetGlobalStep), move.midiNote(),
                        move.visibleStep());
            }
        }
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            observedClip.clearStep(move.sourceFineStart(), move.midiNote());
        }
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            observedClip.setStep(move.targetFineStart(), move.midiNote(), move.velocity(), move.duration());
            eventIndex.moveFineStart(move.sourceFineStart(), move.targetFineStart(), move.midiNote(), move.duration());
        }
        beginMoveInFlight();
        refreshObservation.run();
    }

    private void beginMoveInFlight() {
        moveInFlight = true;
        final int generation = ++moveGeneration;
        scheduleTask.accept(() -> {
            if (moveGeneration == generation) {
                moveInFlight = false;
            }
        }, 24);
    }
}
