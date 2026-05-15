package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.sequence.RecurrencePattern;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class ChordStepPadController {
    public interface Host {
        void handleClipRowPad(int padIndex, boolean pressed);

        boolean isBuilderFamily();

        boolean hasChordSlot(int sourcePadIndex);

        void selectChordSlot(int sourcePadIndex);

        boolean isStepAuditionEnabled();

        boolean isTransportPlaying();

        void startAuditionSelectedChord(int velocity);

        void startAuditionSelectedChord();

        void stopAuditionNotes();

        int currentChordVelocity(int velocity);

        void assignSelectedChordToHeldSteps(int velocity);

        boolean assignSelectedChordToSteps(Set<Integer> stepIndexes, int velocity);

        void showCurrentChord();

        void toggleBuilderNoteOffset(int sourcePadIndex);

        void applyBuilderToHeldSteps();

        List<NoteStep> heldNotes();

        void applyChordStepRecurrence(List<NoteStep> targets, UnaryOperator<RecurrencePattern> updater);

        boolean ensureSelectedNoteClipSlot();

        boolean isAccentGestureActive();

        void toggleAccentForStep(int stepIndex);

        boolean isSelectHeld();

        boolean isFixedLengthHeld();

        boolean isCopyHeld();

        boolean isDeleteHeld();

        void selectStep(int stepIndex);

        void setLastStep(int stepIndex);

        void pasteCurrentChordToStep(int stepIndex);

        void clearChordStep(int stepIndex);

        boolean canExtendHeldChordRange(int anchorStepIndex, int targetStepIndex);

        boolean extendHeldChordRange(int anchorStepIndex, int targetStepIndex);

        void showExtendedStepInfo(int anchorStepIndex, int targetStepIndex);

        void showBlockedStepInfo();

        boolean hasStepStartNote(int stepIndex);

        void loadBuilderFromStep(int stepIndex);

        void showHeldStepInfo(int stepIndex);

        void removeHeldBankFineStart(int stepIndex);
    }

    private final ChordStepPadSurface padSurface;
    private final int clipRowPadCount;
    private final int chordSourcePadOffset;
    private final int stepPadOffset;
    private final Host host;
    private final ChordStepPadSurface.StepPadCallbacks stepPadCallbacks = new StepPadCallbacks();

    public ChordStepPadController(final ChordStepPadSurface padSurface,
                                  final int clipRowPadCount,
                                  final int chordSourcePadOffset,
                                  final int stepPadOffset,
                                  final Host host) {
        this.padSurface = padSurface;
        this.clipRowPadCount = clipRowPadCount;
        this.chordSourcePadOffset = chordSourcePadOffset;
        this.stepPadOffset = stepPadOffset;
        this.host = host;
    }

    public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
        if (padIndex < clipRowPadCount) {
            handleClipRowPadPress(padIndex, pressed);
            return;
        }
        if (padIndex < stepPadOffset) {
            handleSourcePadPress(padIndex - chordSourcePadOffset, pressed, velocity);
            return;
        }
        padSurface.handleStepPadPress(padIndex - stepPadOffset, pressed, velocity, stepPadCallbacks);
    }

    private void handleClipRowPadPress(final int padIndex, final boolean pressed) {
        if (padSurface.hasHeldSteps() && handleRecurrencePadPress(padIndex, pressed)) {
            return;
        }
        host.handleClipRowPad(padIndex, pressed);
    }

    private void handleSourcePadPress(final int sourcePadIndex, final boolean pressed, final int velocity) {
        if (host.isBuilderFamily()) {
            handleBuilderSourcePadPress(sourcePadIndex, pressed);
            return;
        }
        if (!host.hasChordSlot(sourcePadIndex)) {
            return;
        }
        if (pressed) {
            host.selectChordSlot(sourcePadIndex);
            final boolean hasHeldSteps = padSurface.hasHeldSteps();
            final boolean auditionEnabled = host.isStepAuditionEnabled();
            final boolean transportStopped = !host.isTransportPlaying();
            if (auditionEnabled && (!hasHeldSteps || transportStopped)) {
                host.startAuditionSelectedChord(host.currentChordVelocity(velocity));
            }
            if (hasHeldSteps) {
                host.assignSelectedChordToHeldSteps(velocity);
            } else if (!auditionEnabled) {
                host.showCurrentChord();
            }
        } else {
            host.stopAuditionNotes();
        }
    }

    private void handleBuilderSourcePadPress(final int sourcePadIndex, final boolean pressed) {
        if (!pressed) {
            host.stopAuditionNotes();
            return;
        }
        host.toggleBuilderNoteOffset(sourcePadIndex);
        host.applyBuilderToHeldSteps();
        if (host.isStepAuditionEnabled()) {
            host.startAuditionSelectedChord();
        } else {
            host.showCurrentChord();
        }
    }

    private boolean handleRecurrencePadPress(final int padIndex, final boolean pressed) {
        final List<NoteStep> targets = host.heldNotes();
        if (targets.isEmpty()) {
            return true;
        }
        return padSurface.handleRecurrencePadPress(padIndex, pressed, targets,
                () -> padSurface.markModifiedSteps(padSurface.heldStepSnapshot()),
                pad -> host.applyChordStepRecurrence(targets, recurrencePattern -> recurrencePattern.toggledAt(pad)),
                span -> host.applyChordStepRecurrence(targets, recurrencePattern -> recurrencePattern.applySpanGesture(span)));
    }

    private final class StepPadCallbacks implements ChordStepPadSurface.StepPadCallbacks {
        @Override
        public boolean ensureSelectedNoteClipSlot() {
            return host.ensureSelectedNoteClipSlot();
        }

        @Override
        public boolean isAccentGestureActive() {
            return host.isAccentGestureActive();
        }

        @Override
        public void toggleAccentForStep(final int stepIndex) {
            host.toggleAccentForStep(stepIndex);
        }

        @Override
        public boolean isSelectHeld() {
            return host.isSelectHeld();
        }

        @Override
        public boolean isFixedLengthHeld() {
            return host.isFixedLengthHeld();
        }

        @Override
        public boolean isCopyHeld() {
            return host.isCopyHeld();
        }

        @Override
        public boolean isDeleteHeld() {
            return host.isDeleteHeld();
        }

        @Override
        public void handleSelectStep(final int stepIndex) {
            host.selectStep(stepIndex);
        }

        @Override
        public void setLastStep(final int stepIndex) {
            host.setLastStep(stepIndex);
        }

        @Override
        public void pasteCurrentChordToStep(final int stepIndex) {
            host.pasteCurrentChordToStep(stepIndex);
        }

        @Override
        public void clearChordStep(final int stepIndex) {
            host.clearChordStep(stepIndex);
        }

        @Override
        public boolean isBuilderFamily() {
            return host.isBuilderFamily();
        }

        @Override
        public boolean canExtendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
            return host.canExtendHeldChordRange(anchorStepIndex, targetStepIndex);
        }

        @Override
        public boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
            return host.extendHeldChordRange(anchorStepIndex, targetStepIndex);
        }

        @Override
        public void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {
            host.showExtendedStepInfo(anchorStepIndex, targetStepIndex);
        }

        @Override
        public void showBlockedStepInfo() {
            host.showBlockedStepInfo();
        }

        @Override
        public boolean hasStepStartNote(final int stepIndex) {
            return host.hasStepStartNote(stepIndex);
        }

        @Override
        public boolean assignSelectedChordToStep(final int stepIndex, final int velocity) {
            return host.assignSelectedChordToSteps(Collections.singleton(stepIndex), velocity);
        }

        @Override
        public void loadBuilderFromStep(final int stepIndex) {
            host.loadBuilderFromStep(stepIndex);
        }

        @Override
        public void showHeldStepInfo(final int stepIndex) {
            host.showHeldStepInfo(stepIndex);
        }

        @Override
        public void removeHeldBankFineStart(final int stepIndex) {
            host.removeHeldBankFineStart(stepIndex);
        }
    }
}
