package com.oikoaudio.fire.chordstep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Owns chord-step fine-nudge session state for held-step, shift-bank, and pending bank-button gestures.
 */
public final class ChordStepFineNudgeState<E> {
    private final Map<Integer, Map<Integer, Integer>> heldFineStarts = new HashMap<>();
    private final Map<Integer, E> heldEvents = new HashMap<>();
    private final Set<Integer> shiftTargetSteps = new HashSet<>();
    private final Map<Integer, Map<Integer, Integer>> shiftFineStarts = new HashMap<>();
    private final Map<Integer, E> shiftEvents = new HashMap<>();
    private final Set<Integer> pendingTargetSteps = new HashSet<>();
    private final Map<Integer, Map<Integer, Integer>> pendingFineStarts = new HashMap<>();
    private final Map<Integer, E> pendingEvents = new HashMap<>();

    private int pendingDirection = 0;
    private boolean pendingFineMove = false;
    private boolean pendingLengthAdjust = false;

    public E heldEvent(final int stepIndex) {
        return heldEvents.get(stepIndex);
    }

    public E shiftEvent(final int stepIndex) {
        return shiftEvents.get(stepIndex);
    }

    public boolean hasShiftEvent(final int stepIndex) {
        return shiftEvents.containsKey(stepIndex);
    }

    public Set<Integer> shiftTargetStepsSnapshot() {
        return Set.copyOf(shiftTargetSteps);
    }

    public boolean hasShiftTargetSteps() {
        return !shiftTargetSteps.isEmpty();
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

    public void putShiftFineStart(final int stepIndex, final int midiNote, final int fineStart) {
        shiftFineStarts.computeIfAbsent(stepIndex, ignored -> new HashMap<>()).put(midiNote, fineStart);
    }

    public void putHeldEvent(final int stepIndex, final E event) {
        heldEvents.put(stepIndex, event);
    }

    public void putShiftEvent(final int stepIndex, final E event) {
        shiftEvents.put(stepIndex, event);
    }

    public void beginHeldNudge(final Set<Integer> targetSteps) {
        heldFineStarts.keySet().retainAll(targetSteps);
        targetSteps.forEach(step -> heldFineStarts.put(step, new HashMap<>()));
        heldEvents.keySet().retainAll(targetSteps);
    }

    public void beginShiftNudge(final Set<Integer> targetSteps) {
        shiftTargetSteps.clear();
        shiftTargetSteps.addAll(targetSteps);
        shiftFineStarts.keySet().retainAll(targetSteps);
        targetSteps.forEach(step -> shiftFineStarts.put(step, new HashMap<>()));
        shiftEvents.keySet().retainAll(targetSteps);
    }

    public void clearHeld() {
        heldFineStarts.clear();
        heldEvents.clear();
    }

    public void clearShift() {
        shiftTargetSteps.clear();
        shiftFineStarts.clear();
        shiftEvents.clear();
    }

    public void clearAll() {
        clearHeld();
        clearShift();
    }

    public void invalidateStep(final int stepIndex) {
        heldFineStarts.remove(stepIndex);
        heldEvents.remove(stepIndex);
        shiftFineStarts.remove(stepIndex);
        shiftEvents.remove(stepIndex);
        pendingFineStarts.remove(stepIndex);
        pendingEvents.remove(stepIndex);
        pendingTargetSteps.remove(stepIndex);
    }

    public void beginPending(final int direction, final boolean fineMove) {
        pendingDirection = direction;
        pendingFineMove = fineMove;
        pendingTargetSteps.clear();
        pendingFineStarts.clear();
        pendingEvents.clear();
    }

    public void clearPending() {
        pendingDirection = 0;
        pendingFineMove = false;
        pendingLengthAdjust = false;
        pendingTargetSteps.clear();
        pendingFineStarts.clear();
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

    public void putPendingFineStarts(final int stepIndex, final Map<Integer, Integer> starts) {
        pendingFineStarts.put(stepIndex, new HashMap<>(starts));
    }

    public Set<Integer> pendingTargetStepsSnapshot() {
        return Set.copyOf(pendingTargetSteps);
    }

    public Map<Integer, E> pendingEventsSnapshot() {
        return new HashMap<>(pendingEvents);
    }
}
