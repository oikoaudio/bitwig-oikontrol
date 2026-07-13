package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.Clip;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Owns low-level chord-step note writes and cache invalidation against the observed Bitwig clip.
 */
final class ChordStepClipEditor<E> {
    private final Clip observedClip;
    private final ChordStepObservedState observedState;
    private final ChordStepFineNudgeSession<E> fineNudgeSession;
    private final IntUnaryOperator localToGlobalStep;
    private final IntUnaryOperator localToGlobalFineStep;
    private final Runnable queueObservationResync;
    private final int fineStepsPerStep;

    public ChordStepClipEditor(
            final Clip observedClip,
            final ChordStepObservedState observedState,
            final ChordStepFineNudgeSession<E> fineNudgeSession,
            final IntUnaryOperator localToGlobalStep,
            final IntUnaryOperator localToGlobalFineStep,
            final Runnable queueObservationResync,
            final int fineStepsPerStep) {
        this.observedClip = observedClip;
        this.observedState = observedState;
        this.fineNudgeSession = fineNudgeSession;
        this.localToGlobalStep = localToGlobalStep;
        this.localToGlobalFineStep = localToGlobalFineStep;
        this.queueObservationResync = queueObservationResync;
        this.fineStepsPerStep = fineStepsPerStep;
    }

    public void writeChordAtStep(
            final int stepIndex, final int[] notes, final int velocity, final double duration) {
        clearChordStep(stepIndex);
        for (final int midiNote : notes) {
            setChordStep(stepIndex, midiNote, velocity, duration);
        }
        invalidateObservedChordStep(stepIndex);
        queueObservationResync.run();
    }

    public void replaceChordPitchesAtFineStart(
            final int stepIndex,
            final int fineStart,
            final int[] notes,
            final int velocity,
            final double duration) {
        clearChordStep(stepIndex);
        for (final int midiNote : notes) {
            observedClip.setStep(fineStart, midiNote, velocity, duration);
        }
        invalidateObservedChordStep(stepIndex);
        queueObservationResync.run();
    }

    public void clearChordStep(final int stepIndex) {
        final int fineStart = localToGlobalFineStep.applyAsInt(stepIndex);
        for (int offset = 0; offset < fineStepsPerStep; offset++) {
            observedClip.clearStepsAtX(0, fineStart + offset);
        }
    }

    public void setChordStep(
            final int stepIndex, final int midiNote, final int velocity, final double duration) {
        observedClip.setStep(
                localToGlobalFineStep.applyAsInt(stepIndex), midiNote, velocity, duration);
    }

    public void clearChordNote(final int stepIndex, final int midiNote) {
        final Map<Integer, Integer> observedStarts =
                observedState.noteStartsForStep(localToGlobalStep.applyAsInt(stepIndex));
        final int fineStart =
                observedStarts.getOrDefault(midiNote, localToGlobalFineStep.applyAsInt(stepIndex));
        observedClip.clearStep(fineStart, midiNote);
        invalidateObservedChordStep(stepIndex);
        queueObservationResync.run();
    }

    public void invalidateObservedChordStep(final int stepIndex) {
        final int globalStep = localToGlobalStep.applyAsInt(stepIndex);
        observedState.invalidateStep(globalStep);
        fineNudgeSession.invalidateStep(stepIndex);
    }
}
