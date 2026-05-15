package com.oikoaudio.fire.chordstep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Owns chord-step fine-nudge session state for held-step bank-button gestures.
 */
public final class ChordStepFineNudgeState<E> {
    private final Map<Integer, Map<Integer, Integer>> heldFineStarts = new HashMap<>();
    private final Map<Integer, E> heldEvents = new HashMap<>();
    private final Set<Integer> pendingTargetSteps = new HashSet<>();
    private final Map<Integer, E> pendingEvents = new HashMap<>();

    private int pendingDirection = 0;
    private boolean pendingFineMove = false;
    private boolean pendingLengthAdjust = false;

    public E heldEvent(final int stepIndex) {
        return heldEvents.get(stepIndex);
    }

    public Map<Integer, Integer> fineStartsForStep(final int stepIndex, final boolean heldOnly) {
        return heldOnly ? heldFineStarts.get(stepIndex) : null;
    }

    public void rememberHeldFineStarts(final int stepIndex, final Map<Integer, Integer> starts) {
        heldFineStarts.put(stepIndex, new HashMap<>(starts));
    }

    public void putHeldFineStart(final int stepIndex, final int midiNote, final int fineStart) {
        heldFineStarts.computeIfAbsent(stepIndex, ignored -> new HashMap<>()).put(midiNote, fineStart);
    }

    public void putHeldEvent(final int stepIndex, final E event) {
        heldEvents.put(stepIndex, event);
    }

    public void beginHeldNudge(final Set<Integer> targetSteps) {
        heldFineStarts.keySet().retainAll(targetSteps);
        targetSteps.forEach(step -> heldFineStarts.put(step, new HashMap<>()));
        heldEvents.keySet().retainAll(targetSteps);
    }

    public void clearHeld() {
        heldFineStarts.clear();
        heldEvents.clear();
    }

    public void invalidateStep(final int stepIndex) {
        heldFineStarts.remove(stepIndex);
        heldEvents.remove(stepIndex);
        pendingEvents.remove(stepIndex);
        pendingTargetSteps.remove(stepIndex);
    }

    public void beginPending(final int direction, final boolean fineMove) {
        pendingDirection = direction;
        pendingFineMove = fineMove;
        pendingTargetSteps.clear();
        pendingEvents.clear();
    }

    public void clearPending() {
        pendingDirection = 0;
        pendingFineMove = false;
        pendingLengthAdjust = false;
        pendingTargetSteps.clear();
        pendingEvents.clear();
    }

    public int pendingDirection() {
        return pendingDirection;
    }

    public boolean isPendingFineMove() {
        return pendingFineMove;
    }

    public void setPendingLengthAdjust(final boolean pendingLengthAdjust) {
        this.pendingLengthAdjust = pendingLengthAdjust;
    }

    public boolean isPendingLengthAdjust() {
        return pendingLengthAdjust;
    }

    public void addPendingTargetSteps(final Set<Integer> targetSteps) {
        pendingTargetSteps.addAll(targetSteps);
    }

    public void putPendingEvent(final int stepIndex, final E event) {
        pendingEvents.put(stepIndex, event);
    }

    public Set<Integer> pendingTargetStepsSnapshot() {
        return Set.copyOf(pendingTargetSteps);
    }

    public Map<Integer, E> pendingEventsSnapshot() {
        return new HashMap<>(pendingEvents);
    }
}
