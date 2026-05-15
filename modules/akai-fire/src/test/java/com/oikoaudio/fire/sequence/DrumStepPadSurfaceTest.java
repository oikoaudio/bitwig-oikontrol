package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.lights.RgbLigthState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DrumStepPadSurfaceTest {
    @Test
    void projectsHeldStepsToActiveAssignedNotes() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();
        final NoteStep[] assignments = new NoteStep[32];
        assignments[2] = note(2, NoteStep.State.NoteOn, 8, 0b11111111);
        assignments[3] = note(3, NoteStep.State.Empty, 8, 0b11111111);

        surface.pressStep(2);
        surface.pressStep(3);
        surface.pressStep(31);

        assertEquals(List.of(assignments[2]), surface.heldNotes(assignments));
    }

    @Test
    void ownsStepPressReleaseHoldLifecycle() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();

        surface.pressStep(6);

        assertTrue(surface.isAnyStepHeld());

        surface.releaseStep(6);

        assertTrue(surface.heldStepStream().toList().isEmpty());
    }

    @Test
    void releaseRequestsClearForUnmodifiedExistingNote() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();

        final DrumStepPadSurface.StepReleaseAction action = surface.handleStepRelease(5,
                note(5, NoteStep.State.NoteOn, 8, 0b11111111),
                false, false, false);

        assertEquals(DrumStepPadSurface.StepReleaseAction.CLEAR_STEP, action);
    }

    @Test
    void releaseKeepsModifiedExistingNoteAndClearsModifiedFlag() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();
        surface.markModified(5);

        final DrumStepPadSurface.StepReleaseAction action = surface.handleStepRelease(5,
                note(5, NoteStep.State.NoteOn, 8, 0b11111111),
                false, false, false);

        assertEquals(DrumStepPadSurface.StepReleaseAction.NONE, action);
        assertEquals(DrumStepPadSurface.StepReleaseAction.CLEAR_STEP, surface.handleStepRelease(5,
                note(5, NoteStep.State.NoteOn, 8, 0b11111111),
                false, false, false));
    }

    @Test
    void releaseDoesNotClearStepsAfterConsumedGesture() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();
        surface.markGestureConsumed(5);

        final DrumStepPadSurface.StepReleaseAction action = surface.handleStepRelease(5,
                note(5, NoteStep.State.NoteOn, 8, 0b11111111),
                false, false, false);

        assertEquals(DrumStepPadSurface.StepReleaseAction.NONE, action);
    }

    @Test
    void pressRequestsAddStepForEmptyStep() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();

        final DrumStepPadSurface.StepPressAction action = surface.handleStepPress(5, null,
                false, false, false);

        assertEquals(DrumStepPadSurface.StepPressAction.ADD_STEP, action);
        assertTrue(surface.isAnyStepHeld());
    }

    @Test
    void appliesDefaultsOnlyToObservedAddedNoteOnSteps() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();
        surface.markAdded(5);

        assertTrue(surface.shouldApplyDefaultsToObservedStep(
                note(5, NoteStep.State.NoteOn, 8, 0b11111111)));
        assertFalse(surface.shouldApplyDefaultsToObservedStep(
                note(6, NoteStep.State.NoteOn, 8, 0b11111111)));
        assertFalse(surface.shouldApplyDefaultsToObservedStep(
                note(5, NoteStep.State.Empty, 8, 0b11111111)));
    }

    @Test
    void pressRequestsNoActionForExistingStep() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();

        final DrumStepPadSurface.StepPressAction action = surface.handleStepPress(5,
                note(5, NoteStep.State.NoteOn, 8, 0b11111111),
                false, false, false);

        assertEquals(DrumStepPadSurface.StepPressAction.NONE, action);
    }

    @Test
    void pressPrioritizesModifierGesturesBeforeAddStep() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();

        assertEquals(DrumStepPadSurface.StepPressAction.FIXED_LENGTH,
                surface.handleStepPress(5, null, true, true, true));
        assertEquals(DrumStepPadSurface.StepPressAction.COPY,
                surface.handleStepPress(6, null, false, true, true));
        assertEquals(DrumStepPadSurface.StepPressAction.ACCENT,
                surface.handleStepPress(7, null, false, false, true));
    }

    @Test
    void routesRecurrencePadPressesThroughSharedInteraction() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();
        final List<Integer> toggles = new ArrayList<>();
        final List<Integer> spans = new ArrayList<>();
        final List<String> consumed = new ArrayList<>();

        assertTrue(surface.handleRecurrencePadPress(3, true,
                List.of(note(5, NoteStep.State.NoteOn, 8, 0b11111111)),
                () -> consumed.add("yes"),
                toggles::add,
                spans::add));

        assertEquals(List.of("yes"), consumed);
        assertEquals(List.of(3), toggles);
        assertEquals(List.of(), spans);
    }

    @Test
    void rendersRecurrencePadLightForHeldTarget() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();
        final RgbLigthState base = RgbLigthState.PURPLE;

        final RgbLigthState light = surface.recurrencePadLight(0,
                List.of(note(5, NoteStep.State.NoteOn, 4, 0b0001)),
                base,
                RgbLigthState.OFF);

        assertSame(base.getBrightend(), light);
    }

    @Test
    void rendersStepPadsOutsideAvailableStepsAsOff() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();

        assertSame(RgbLigthState.OFF, surface.stepPadLight(12, 8, null, 0,
                false, null, 0, RgbLigthState.PURPLE, 110));
    }

    @Test
    void rendersEmptyStepPadsWithSharedEmptyPolicy() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();

        assertSame(RgbLigthState.WHITE, surface.stepPadLight(4, 16, null, 4,
                false, null, 0, RgbLigthState.PURPLE, 110));
        assertSame(RgbLigthState.GRAY_2, surface.stepPadLight(5, 16, null, 4,
                false, null, 0, RgbLigthState.PURPLE, 110));
    }

    @Test
    void rendersAccentedOccupiedStepPadsBright() {
        final DrumStepPadSurface surface = new DrumStepPadSurface();
        final RgbLigthState base = RgbLigthState.PURPLE;

        final RgbLigthState light = surface.stepPadLight(5, 16,
                note(5, NoteStep.State.NoteOn, 8, 0b11111111, 1.0),
                0, false, null, 0, base, 110);

        assertSame(base.getBrightend(), light);
    }

    private static NoteStep note(final int x,
                                 final NoteStep.State state,
                                 final int recurrenceLength,
                                 final int recurrenceMask) {
        return note(x, state, recurrenceLength, recurrenceMask, 0.5);
    }

    private static NoteStep note(final int x,
                                 final NoteStep.State state,
                                 final int recurrenceLength,
                                 final int recurrenceMask,
                                 final double velocity) {
        final NoteStep note = mock(NoteStep.class);
        when(note.x()).thenReturn(x);
        when(note.state()).thenReturn(state);
        when(note.recurrenceLength()).thenReturn(recurrenceLength);
        when(note.recurrenceMask()).thenReturn(recurrenceMask);
        when(note.velocity()).thenReturn(velocity);
        return note;
    }
}
