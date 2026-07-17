package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.bitwig.extensions.framework.values.Midi;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChordStepWorkflowScenarioTest {
    private static final int FINE_STEPS_PER_STEP = 16;

    @Test
    void newChordPadPressSurvivesReleaseAndChangesFromHeldToOccupiedFeedback() {
        final ChordStepPadSurface pads = new ChordStepPadSurface();
        final Set<Integer> occupiedSteps = new HashSet<>();
        final ChordStepPadSurface.StepPadCallbacks editor = new RecordingChordEditor(occupiedSteps);
        final ChordStepPadLightRenderer lights = padLights(pads, occupiedSteps);

        pads.handleStepPadPress(2, true, 96, editor);

        assertEquals(
                new RgbLightState(120, 88, 0, true).getBrightest(),
                lights.padLight(34, 16, 16, 32),
                "a pressed chord step should use held-pad feedback");

        pads.handleStepPadPress(2, false, 0, editor);

        assertTrue(occupiedSteps.contains(2), "releasing a newly written chord must keep it");
        assertEquals(
                RgbLightState.PURPLE,
                lights.padLight(34, 16, 16, 32),
                "the released pad should show occupied-step feedback");
    }

    @Test
    void heldStepBankGestureRewritesItsEventByOneFineStep() {
        final Clip clip = mock(Clip.class);
        final ChordStepEventIndex eventIndex = eventIndex();
        final ChordStepEventIndex.Event event = eventAtFineStep(2, 32);
        final AtomicReference<ChordStepFineNudgeWriter> writerRef = new AtomicReference<>();
        final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session =
                new ChordStepFineNudgeSession<>(
                        step -> step == 2 ? event : null,
                        (amount, steps, events) ->
                                writerRef.get().nudgeHeldNotes(amount, steps, events));
        final ChordStepFineNudgeWriter writer =
                fineNudgeWriter(clip, eventIndex, session, task -> {});
        writerRef.set(writer);
        final ChordStepBankButtonControls bank =
                new ChordStepBankButtonControls(new BankGestureHost(session, writer, Set.of(2)));

        bank.handlePressed(true, 1, true);
        bank.handlePressed(false, 1, true);

        assertEquals(
                Map.of(60, 33),
                session.fineStartsForStep(2, true),
                "BANK-right should move the held chord event by one fine step");
        assertTrue(writer.isMoveInFlight(), "the rewritten event should await observation");
    }

    @Test
    void selectedClipRefreshUpdatesSelectionAndVisibleChordState() {
        final ChordStepVisibleClipCache visible = new ChordStepVisibleClipCache(32);
        final List<Integer> scheduledDelays = new ArrayList<>();
        final AtomicReference<ChordStepObservationController> observationRef =
                new AtomicReference<>();
        final ChordStepClipController clip =
                new ChordStepClipController(
                        () -> true,
                        () -> false,
                        () -> observationRef.get().queueResync(),
                        failure -> {});
        final ChordStepObservationController observation =
                new ChordStepObservationController(
                        (task, delay) -> scheduledDelays.add(delay),
                        null,
                        () -> 3,
                        () -> RgbLightState.GRAY_1,
                        clip,
                        () -> {},
                        () -> {},
                        () -> {},
                        () -> {},
                        () -> {},
                        (slotBank,
                                selectedIndex,
                                refreshSelection,
                                slotIndex,
                                scrollNoteKey,
                                scrollObservedKey,
                                scrollNoteStep,
                                scrollObservedStep) ->
                                visible.handleStepData(4, 60, NoteStep.State.NoteOn.ordinal()),
                        (slotBank, defaultColor) ->
                                SelectedClipSlotState.fromValues(3, true, RgbLightState.PURPLE));
        observationRef.set(observation);

        observation.refresh();

        assertEquals(3, clip.slotIndex(), "the selected clip index should be refreshed");
        assertTrue(clip.hasContent(), "the selected clip should report note content");
        assertEquals(RgbLightState.PURPLE, clip.color(), "clip color should follow selection");
        assertEquals(
                Set.of(60),
                visible.notesAtStep(4),
                "the refresh pass should expose visible chord notes");
        assertEquals(
                List.of(0, 1, 6, 18),
                scheduledDelays,
                "clip change and delayed observation passes should retain their timings");
    }

    @Test
    void builderPadSelectionAuditionsTheRenderedChordAndUpdatesDisplayState() {
        final SharedPitchContextController pitch = pitchContext();
        final ChordStepChordSelection selection = new ChordStepChordSelection();
        final ChordStepBuilderController builder =
                new ChordStepBuilderController(selection, pitch, () -> 60, 16);
        final List<String> midi = new ArrayList<>();
        final NoteInput noteInput = recordingNoteInput(midi);
        final ChordStepAuditionController audition = new ChordStepAuditionController(noteInput);
        final List<String> display = new ArrayList<>();

        builder.handleSourcePad(0, true);
        final int[] rendered =
                selection.renderSelectedChord(pitch.getMusicalScale(), pitch.getRootNote());
        audition.startAudition(rendered, 96);
        display.add(selection.familyLabel() + " " + Arrays.toString(rendered));

        assertEquals(List.of("note-on 60 96"), midi, "the selected builder note should audition");
        assertEquals(
                List.of("Builder [60]"),
                display,
                "display feedback should describe the rendered builder chord");
    }

    @Test
    void cancellationClearsHeldPadsPendingGestureAndDelayedMoveState() {
        final ChordStepPadSurface pads = new ChordStepPadSurface();
        pads.addHeldStep(2);
        final ChordStepEventIndex.Event event = eventAtFineStep(2, 32);
        final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session =
                new ChordStepFineNudgeSession<>(step -> event, (amount, steps, events) -> {});
        session.putHeldEvent(2, event);
        session.beginHeldNudge(1, Set.of(2));
        final AtomicReference<Runnable> delayed = new AtomicReference<>();
        final ChordStepFineNudgeWriter writer =
                fineNudgeWriter(mock(Clip.class), eventIndex(), session, delayed::set);
        writer.nudgeHeldNotes(1, Set.of(2), Map.of(2, event));

        pads.clearStepTracking();
        session.clear();
        writer.cancelPendingMove();
        delayed.get().run();

        assertFalse(pads.hasHeldSteps(), "deactivation should release held pads");
        assertNull(session.heldEvent(2), "deactivation should forget the held event snapshot");
        assertFalse(session.completePendingNudge(), "a cancelled BANK gesture must not complete");
        assertFalse(writer.isMoveInFlight(), "a stale delayed task must not revive move state");
    }

    private static ChordStepPadLightRenderer padLights(
            final ChordStepPadSurface pads, final Set<Integer> occupiedSteps) {
        final SharedPitchContextController pitch = pitchContext();
        final ChordStepChordSelection selection = new ChordStepChordSelection();
        final ChordStepBuilderController builder =
                new ChordStepBuilderController(selection, pitch, () -> 60, 16);
        return new ChordStepPadLightRenderer(
                pads,
                builder,
                selection,
                ignored -> RgbLightState.GRAY_1,
                List::of,
                () -> RgbLightState.PURPLE,
                () -> 32,
                () -> -1,
                () -> -1,
                occupiedSteps::contains,
                step -> false,
                step -> false);
    }

    private static SharedPitchContextController pitchContext() {
        final MusicalScaleLibrary library = MusicalScaleLibrary.getInstance();
        final SharedPitchContextController pitch =
                new SharedPitchContextController(new SharedMusicalContext(library), library);
        pitch.initializeFromPreferences(FireControlPreferences.DEFAULT_SCALE_MAJOR, 0, 3);
        return pitch;
    }

    private static NoteInput recordingNoteInput(final List<String> midi) {
        final NoteInput noteInput = mock(NoteInput.class);
        doAnswer(
                        invocation -> {
                            final int status = invocation.getArgument(0);
                            final int note = invocation.getArgument(1);
                            final int velocity = invocation.getArgument(2);
                            midi.add(
                                    (status == Midi.NOTE_ON ? "note-on " : "note-off ")
                                            + note
                                            + " "
                                            + velocity);
                            return null;
                        })
                .when(noteInput)
                .sendRawMidiEvent(anyInt(), anyInt(), anyInt());
        return noteInput;
    }

    private static ChordStepFineNudgeWriter fineNudgeWriter(
            final Clip clip,
            final ChordStepEventIndex eventIndex,
            final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session,
            final java.util.function.Consumer<Runnable> delayedTask) {
        return new ChordStepFineNudgeWriter(
                clip,
                eventIndex,
                session,
                steps -> {},
                () -> 512,
                global -> global >= 0 && global < 32,
                global -> global,
                (task, delay) -> delayedTask.accept(task),
                () -> {},
                FINE_STEPS_PER_STEP);
    }

    private static ChordStepEventIndex eventIndex() {
        return new ChordStepEventIndex(
                local -> local,
                global -> global,
                global -> global >= 0 && global < 32,
                ignored -> 96,
                FINE_STEPS_PER_STEP,
                0.25 / FINE_STEPS_PER_STEP,
                0.25);
    }

    private static ChordStepEventIndex.Event eventAtFineStep(
            final int localStep, final int fineStart) {
        return new ChordStepEventIndex.Event(
                localStep,
                localStep,
                fineStart,
                List.of(new ChordStepEventIndex.EventNote(60, fineStart, 0, 96, 0.25, null)));
    }

    private static final class RecordingChordEditor
            implements ChordStepPadSurface.StepPadCallbacks {
        private final Set<Integer> occupiedSteps;

        private RecordingChordEditor(final Set<Integer> occupiedSteps) {
            this.occupiedSteps = occupiedSteps;
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
        public void toggleAccentForStep(final int stepIndex) {}

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
        public boolean hasHeldSourcePads() {
            return false;
        }

        @Override
        public void handleSelectStep(final int stepIndex) {}

        @Override
        public void setLastStep(final int stepIndex) {}

        @Override
        public void pasteCurrentChordToStep(final int stepIndex) {}

        @Override
        public void clearChordStep(final int stepIndex) {
            occupiedSteps.remove(stepIndex);
        }

        @Override
        public boolean isBuilderFamily() {
            return false;
        }

        @Override
        public boolean canExtendHeldChordRange(
                final int anchorStepIndex, final int targetStepIndex) {
            return true;
        }

        @Override
        public boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
            return false;
        }

        @Override
        public void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {}

        @Override
        public void showBlockedStepInfo() {}

        @Override
        public boolean hasStepStartNote(final int stepIndex) {
            return occupiedSteps.contains(stepIndex);
        }

        @Override
        public void loadBuilderFromStep(final int stepIndex) {}

        @Override
        public boolean assignSelectedChordToStep(final int stepIndex, final int velocity) {
            occupiedSteps.add(stepIndex);
            return true;
        }

        @Override
        public boolean replaceSelectedChordAtStep(final int stepIndex) {
            occupiedSteps.add(stepIndex);
            return true;
        }

        @Override
        public void showHeldStepInfo(final int stepIndex) {}

        @Override
        public void removeHeldBankFineStart(final int stepIndex) {}
    }

    private static final class BankGestureHost implements ChordStepBankButtonControls.Host {
        private final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session;
        private final ChordStepFineNudgeWriter writer;
        private final Set<Integer> heldSteps;

        private BankGestureHost(
                final ChordStepFineNudgeSession<ChordStepEventIndex.Event> session,
                final ChordStepFineNudgeWriter writer,
                final Set<Integer> heldSteps) {
            this.session = session;
            this.writer = writer;
            this.heldSteps = heldSteps;
        }

        @Override
        public boolean isAltHeld() {
            return false;
        }

        @Override
        public boolean isShiftHeld() {
            return false;
        }

        @Override
        public void setPendingLengthAdjust(final boolean pending) {
            session.setPendingLengthAdjust(pending);
        }

        @Override
        public boolean isPendingLengthAdjust() {
            return session.isPendingLengthAdjust();
        }

        @Override
        public void adjustLength(final int amount) {}

        @Override
        public boolean isFineNudgeMoveInFlight() {
            return writer.isMoveInFlight();
        }

        @Override
        public Set<Integer> heldStepSnapshot() {
            return heldSteps;
        }

        @Override
        public void beginHeldFineNudge(final int amount, final Set<Integer> steps) {
            session.beginHeldNudge(amount, steps);
        }

        @Override
        public void adjustPlayStart(final int amount, final boolean fine) {}

        @Override
        public void snapPlayStartToGrid() {}

        @Override
        public boolean completePendingFineNudge() {
            return session.completePendingNudge();
        }

        @Override
        public void clearPendingBankAction() {
            session.cancelPending();
        }
    }
}
