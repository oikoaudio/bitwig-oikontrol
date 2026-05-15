package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteStep;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

final class DrumStepPadState {
    private final IntSetValue heldSteps = new IntSetValue();
    private final Set<Integer> addedSteps = new HashSet<>();
    private final Set<Integer> modifiedSteps = new HashSet<>();
    private final Set<Integer> gestureConsumedSteps = new HashSet<>();

    IntSetValue heldStepsValue() {
        return heldSteps;
    }

    Stream<Integer> heldStepStream() {
        return heldSteps.stream();
    }

    boolean isAnyStepHeld() {
        return heldSteps.stream().findAny().isPresent();
    }

    void addHeldStep(final int stepIndex) {
        heldSteps.add(stepIndex);
    }

    void removeHeldStep(final int stepIndex) {
        heldSteps.remove(stepIndex);
    }

    void markAdded(final int stepIndex) {
        addedSteps.add(stepIndex);
    }

    boolean isAdded(final int stepIndex) {
        return addedSteps.contains(stepIndex);
    }

    void removeAdded(final int stepIndex) {
        addedSteps.remove(stepIndex);
    }

    void markModified(final int stepIndex) {
        modifiedSteps.add(stepIndex);
    }

    void markModified(final NoteStep noteStep) {
        modifiedSteps.add(noteStep.x());
    }

    boolean isModified(final int stepIndex) {
        return modifiedSteps.contains(stepIndex);
    }

    void removeModified(final int stepIndex) {
        modifiedSteps.remove(stepIndex);
    }

    void markGestureConsumed(final int stepIndex) {
        gestureConsumedSteps.add(stepIndex);
    }

    boolean consumeGesture(final int stepIndex) {
        return gestureConsumedSteps.remove(stepIndex);
    }

    void clearGestureConsumed(final int stepIndex) {
        gestureConsumedSteps.remove(stepIndex);
    }
}
