package com.oikoaudio.fire.chordstep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Owns the held and pending state for one Poly Step fine-nudge interaction session. */
final class ChordStepFineNudgeSession<E> {
    private final EventSnapshot<E> eventSnapshot;
    private final FineNudgeAction<E> fineNudgeAction;
    private final Map<Integer, Map<Integer, Integer>> heldFineStarts = new HashMap<>();
    private final Map<Integer, E> heldEvents = new HashMap<>();
    private final Set<Integer> pendingTargetSteps = new HashSet<>();
    private final Map<Integer, E> pendingEvents = new HashMap<>();

    private int pendingDirection;
    private boolean pendingFineMove;
    private boolean pendingLengthAdjust;

    public ChordStepFineNudgeSession(
            final EventSnapshot<E> eventSnapshot, final FineNudgeAction<E> fineNudgeAction) {
        this.eventSnapshot = eventSnapshot;
        this.fineNudgeAction = fineNudgeAction;
    }

    public void beginHeldNudge(final int direction, final Set<Integer> targetSteps) {
        clearPending();
        pendingDirection = direction;
        pendingFineMove = true;
        pendingTargetSteps.addAll(targetSteps);
        for (final int stepIndex : targetSteps) {
            final E event = eventSnapshot.snapshot(stepIndex);
            if (event != null) {
                pendingEvents.put(stepIndex, event);
            }
        }
    }

    public boolean completePendingNudge() {
        if (!pendingFineMove) {
            return false;
        }
        fineNudgeAction.nudge(
                pendingDirection, Set.copyOf(pendingTargetSteps), new HashMap<>(pendingEvents));
        clearPending();
        return true;
    }

    public void cancelPending() {
        clearPending();
    }

    public void setPendingLengthAdjust(final boolean pending) {
        if (pending) {
            clearPending();
            pendingLengthAdjust = true;
        } else {
            pendingLengthAdjust = false;
        }
    }

    public boolean isPendingLengthAdjust() {
        return pendingLengthAdjust;
    }

    public E heldEvent(final int stepIndex) {
        return heldEvents.get(stepIndex);
    }

    public Map<Integer, Integer> fineStartsForStep(final int stepIndex, final boolean heldOnly) {
        return heldOnly ? heldFineStarts.get(stepIndex) : null;
    }

    public void rememberHeldFineStarts(final int stepIndex, final Map<Integer, Integer> starts) {
        heldFineStarts.put(stepIndex, new HashMap<>(starts));
    }

    public void prepareHeldMove(final Set<Integer> targetSteps) {
        heldFineStarts.keySet().retainAll(targetSteps);
        targetSteps.forEach(step -> heldFineStarts.put(step, new HashMap<>()));
        heldEvents.keySet().retainAll(targetSteps);
    }

    public void putHeldFineStart(final int stepIndex, final int midiNote, final int fineStart) {
        heldFineStarts
                .computeIfAbsent(stepIndex, ignored -> new HashMap<>())
                .put(midiNote, fineStart);
    }

    public void putHeldEvent(final int stepIndex, final E event) {
        heldEvents.put(stepIndex, event);
    }

    public void invalidateStep(final int stepIndex) {
        heldFineStarts.remove(stepIndex);
        heldEvents.remove(stepIndex);
        pendingEvents.remove(stepIndex);
        pendingTargetSteps.remove(stepIndex);
    }

    public void clearHeld() {
        heldFineStarts.clear();
        heldEvents.clear();
    }

    public void clear() {
        clearPending();
        clearHeld();
    }

    private void clearPending() {
        pendingDirection = 0;
        pendingFineMove = false;
        pendingLengthAdjust = false;
        pendingTargetSteps.clear();
        pendingEvents.clear();
    }

    @FunctionalInterface
    public interface EventSnapshot<E> {
        E snapshot(int stepIndex);
    }

    @FunctionalInterface
    public interface FineNudgeAction<E> {
        void nudge(int direction, Set<Integer> targetSteps, Map<Integer, E> events);
    }
}
