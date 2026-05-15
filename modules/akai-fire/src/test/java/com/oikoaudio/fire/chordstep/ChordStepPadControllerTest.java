package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.sequence.RecurrencePattern;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChordStepPadControllerTest {
    private static final int CLIP_ROW_PAD_COUNT = 16;
    private static final int CHORD_SOURCE_PAD_OFFSET = 16;
    private static final int STEP_PAD_OFFSET = 32;

    @Test
    void delegatesClipRowPadsWhenNoStepIsHeld() {
        final FakeHost host = new FakeHost();
        final ChordStepPadController controller = controller(host);

        controller.handlePadPress(3, true, 100);

        assertEquals(List.of("clip 3 true"), host.events);
    }

    @Test
    void sourcePadsSelectAndAssignChordToHeldSteps() {
        final FakeHost host = new FakeHost();
        host.chordSlots.add(2);
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final ChordStepPadController controller = controller(surface, host);
        surface.addHeldStep(5);

        controller.handlePadPress(CHORD_SOURCE_PAD_OFFSET + 2, true, 99);

        assertEquals(2, host.selectedChordSlot);
        assertEquals(List.of("assign-held 99"), host.events);
    }

    @Test
    void builderSourcePadsToggleBuilderAndApplyHeldSteps() {
        final FakeHost host = new FakeHost();
        host.builderFamily = true;
        final ChordStepPadController controller = controller(host);

        controller.handlePadPress(CHORD_SOURCE_PAD_OFFSET + 4, true, 100);

        assertEquals(List.of("toggle-builder 4", "apply-builder", "show-chord"), host.events);
    }

    @Test
    void stepPadsUseSurfaceWorkflowThroughHostCallbacks() {
        final FakeHost host = new FakeHost();
        final ChordStepPadController controller = controller(host);

        controller.handlePadPress(STEP_PAD_OFFSET + 6, true, 90);

        assertEquals(Set.of(6), host.assignedSteps);
        assertEquals(90, host.assignedVelocity);
        assertEquals(List.of("show-held 6"), host.events);
    }

    @Test
    void heldClipRowPadsEditRecurrenceInsteadOfLaunchingClips() {
        final FakeHost host = new FakeHost();
        host.heldNotes = List.of(note(5, 8, 0b00000001));
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final ChordStepPadController controller = controller(surface, host);
        surface.addHeldStep(5);

        controller.handlePadPress(2, true, 100);

        assertEquals(1, host.recurrenceEdits);
        assertTrue(host.events.isEmpty());
    }

    private static ChordStepPadController controller(final FakeHost host) {
        return controller(new ChordStepPadSurface(), host);
    }

    private static ChordStepPadController controller(final ChordStepPadSurface surface, final FakeHost host) {
        return new ChordStepPadController(surface, CLIP_ROW_PAD_COUNT, CHORD_SOURCE_PAD_OFFSET, STEP_PAD_OFFSET, host);
    }

    private static NoteStep note(final int x, final int recurrenceLength, final int recurrenceMask) {
        final NoteStep note = mock(NoteStep.class);
        when(note.x()).thenReturn(x);
        when(note.recurrenceLength()).thenReturn(recurrenceLength);
        when(note.recurrenceMask()).thenReturn(recurrenceMask);
        return note;
    }

    static final class FakeHost implements ChordStepPadController.Host {
        final List<String> events = new ArrayList<>();
        private final Set<Integer> chordSlots = new HashSet<>();
        private List<NoteStep> heldNotes = List.of();
        private boolean builderFamily;
        private int selectedChordSlot = -1;
        private Set<Integer> assignedSteps = Set.of();
        private int assignedVelocity;
        private int recurrenceEdits;

        @Override
        public void handleClipRowPad(final int padIndex, final boolean pressed) {
            events.add("clip " + padIndex + " " + pressed);
        }

        @Override
        public boolean isBuilderFamily() {
            return builderFamily;
        }

        @Override
        public boolean hasChordSlot(final int sourcePadIndex) {
            return chordSlots.contains(sourcePadIndex);
        }

        @Override
        public void selectChordSlot(final int sourcePadIndex) {
            selectedChordSlot = sourcePadIndex;
        }

        @Override
        public boolean isStepAuditionEnabled() {
            return false;
        }

        @Override
        public boolean isTransportPlaying() {
            return false;
        }

        @Override
        public void startAuditionSelectedChord(final int velocity) {
            events.add("audition " + velocity);
        }

        @Override
        public void startAuditionSelectedChord() {
            events.add("audition");
        }

        @Override
        public void stopAuditionNotes() {
            events.add("stop-audition");
        }

        @Override
        public int currentChordVelocity(final int velocity) {
            return velocity;
        }

        @Override
        public void assignSelectedChordToHeldSteps(final int velocity) {
            events.add("assign-held " + velocity);
        }

        @Override
        public boolean assignSelectedChordToSteps(final Set<Integer> stepIndexes, final int velocity) {
            assignedSteps = Set.copyOf(stepIndexes);
            assignedVelocity = velocity;
            return true;
        }

        @Override
        public void showCurrentChord() {
            events.add("show-chord");
        }

        @Override
        public void toggleBuilderNoteOffset(final int sourcePadIndex) {
            events.add("toggle-builder " + sourcePadIndex);
        }

        @Override
        public void applyBuilderToHeldSteps() {
            events.add("apply-builder");
        }

        @Override
        public List<NoteStep> heldNotes() {
            return heldNotes;
        }

        @Override
        public void applyChordStepRecurrence(final List<NoteStep> targets,
                                             final UnaryOperator<RecurrencePattern> updater) {
            recurrenceEdits++;
            updater.apply(RecurrencePattern.of(targets.get(0).recurrenceLength(), targets.get(0).recurrenceMask()));
        }

        @Override
        public boolean ensureSelectedNoteClipSlot() {
            return true;
        }

        @Override
        public boolean isAccentGestureActive() {
            return false;
        }

        @Override
        public void toggleAccentForStep(final int stepIndex) {
            events.add("accent " + stepIndex);
        }

        @Override
        public boolean isSelectHeld() {
            return false;
        }

        @Override
        public boolean isFixedLengthHeld() {
            return false;
        }

        @Override
        public boolean isCopyHeld() {
            return false;
        }

        @Override
        public boolean isDeleteHeld() {
            return false;
        }

        @Override
        public void selectStep(final int stepIndex) {
            events.add("select " + stepIndex);
        }

        @Override
        public void setLastStep(final int stepIndex) {
            events.add("last " + stepIndex);
        }

        @Override
        public void pasteCurrentChordToStep(final int stepIndex) {
            events.add("paste " + stepIndex);
        }

        @Override
        public void clearChordStep(final int stepIndex) {
            events.add("clear " + stepIndex);
        }

        @Override
        public boolean canExtendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
            return true;
        }

        @Override
        public boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
            events.add("extend " + anchorStepIndex + " " + targetStepIndex);
            return true;
        }

        @Override
        public void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {
            events.add("show-extend " + anchorStepIndex + " " + targetStepIndex);
        }

        @Override
        public void showBlockedStepInfo() {
            events.add("blocked");
        }

        @Override
        public boolean hasStepStartNote(final int stepIndex) {
            return false;
        }

        @Override
        public void loadBuilderFromStep(final int stepIndex) {
            events.add("load-builder " + stepIndex);
        }

        @Override
        public void showHeldStepInfo(final int stepIndex) {
            events.add("show-held " + stepIndex);
        }

        @Override
        public void removeHeldBankFineStart(final int stepIndex) {
            events.add("remove-fine " + stepIndex);
        }
    }
}
