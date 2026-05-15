package com.oikoaudio.fire.chordstep;

import java.util.Map;
import java.util.Set;

/**
 * Owns the held-step fine-nudge bank-button gesture lifecycle.
 */
public final class ChordStepFineNudgeController<E> {
    private final ChordStepFineNudgeState<E> state;
    private final EventSnapshot<E> eventSnapshot;
    private final FineNudgeAction<E> fineNudgeAction;

    public ChordStepFineNudgeController(final ChordStepFineNudgeState<E> state,
                                        final EventSnapshot<E> eventSnapshot,
                                        final FineNudgeAction<E> fineNudgeAction) {
        this.state = state;
        this.eventSnapshot = eventSnapshot;
        this.fineNudgeAction = fineNudgeAction;
    }

    public void beginHeldNudge(final int direction, final Set<Integer> targetSteps) {
        state.beginPending(direction, true);
        state.addPendingTargetSteps(targetSteps);
        for (final int stepIndex : targetSteps) {
            final E event = eventSnapshot.snapshot(stepIndex);
            if (event == null) {
                continue;
            }
            state.putPendingEvent(stepIndex, event);
        }
    }

    public boolean completePendingNudge() {
        if (!state.isPendingFineMove()) {
            return false;
        }
        fineNudgeAction.nudge(
                state.pendingDirection(),
                state.pendingTargetStepsSnapshot(),
                state.pendingEventsSnapshot());
        state.clearPending();
        return true;
    }

    public void cancelPending() {
        state.clearPending();
    }

    public void clearHeld() {
        state.clearHeld();
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
