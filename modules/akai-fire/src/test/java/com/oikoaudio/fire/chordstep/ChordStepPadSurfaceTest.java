package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.lights.RgbLigthState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChordStepPadSurfaceTest {
    @Test
    void routesRecurrencePadPressesThroughSharedInteraction() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final List<String> consumed = new ArrayList<>();
        final List<Integer> toggles = new ArrayList<>();
        final List<Integer> spans = new ArrayList<>();

        assertTrue(surface.handleRecurrencePadPress(3, true,
                List.of(note(5, 8, 0b11111111)),
                () -> consumed.add("yes"),
                toggles::add,
                spans::add));

        assertEquals(List.of("yes"), consumed);
        assertEquals(List.of(3), toggles);
        assertEquals(List.of(), spans);
    }

    @Test
    void rendersRecurrencePadLightForTarget() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final RgbLigthState base = RgbLigthState.PURPLE;

        final RgbLigthState light = surface.recurrencePadLight(0,
                List.of(note(5, 4, 0b0001)),
                base,
                RgbLigthState.OFF);

        assertSame(base.getBrightend(), light);
    }

    @Test
    void fallsBackWhenRecurrenceHasNoTarget() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        assertSame(RgbLigthState.GRAY_1, surface.recurrencePadLight(0, List.of(),
                RgbLigthState.PURPLE,
                RgbLigthState.GRAY_1));
    }

    @Test
    void rendersHeldStepAsBrightestHeldColor() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final RgbLigthState held = new RgbLigthState(120, 88, 0, true);
        surface.addHeldStep(5);

        assertSame(held.getBrightest(), surface.stepPadLight(5, 16,
                true, false, false, 0,
                RgbLigthState.PURPLE, RgbLigthState.GRAY_1, held));
    }

    @Test
    void rendersOccupiedAccentedStepBright() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final RgbLigthState occupied = RgbLigthState.PURPLE;

        assertSame(occupied.getBrightend(), surface.stepPadLight(5, 16,
                true, true, false, 0,
                occupied, RgbLigthState.GRAY_1, new RgbLigthState(120, 88, 0, true)));
    }

    @Test
    void rendersOutsideLoopAsOff() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        assertSame(RgbLigthState.OFF, surface.stepPadLight(17, 16,
                true, true, false, 0,
                RgbLigthState.PURPLE, RgbLigthState.GRAY_1, new RgbLigthState(120, 88, 0, true)));
    }

    @Test
    void ownsChordStepTrackingSetsAndAnchor() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        surface.addHeldStep(2);
        surface.markAddedStep(2);
        surface.markModifiedStep(2);
        surface.markModifierHandledStep(2);
        surface.setHeldStepAnchor(2);

        assertEquals(2, surface.heldStepAnchor());
        assertEquals(List.of(2), List.copyOf(surface.heldStepSnapshot()));

        surface.clearStepTracking();

        assertNull(surface.heldStepAnchor());
        assertTrue(surface.heldStepSnapshot().isEmpty());
        assertFalse(surface.hasAddedStep(2));
        assertFalse(surface.consumeModifiedStep(2));
        assertFalse(surface.consumeModifierHandledStep(2));
    }

    @Test
    void refreshesAnchorToRemainingHeldStep() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        surface.addHeldStep(2);
        surface.addHeldStep(5);
        surface.setHeldStepAnchor(2);
        surface.removeHeldStep(2);

        surface.refreshHeldStepAnchor(2);

        assertEquals(5, surface.heldStepAnchor());
    }

    @Test
    void consumesStepTrackingFlagsOnce() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        surface.markAddedStep(2);
        surface.markModifiedStep(3);
        surface.markModifierHandledStep(4);

        assertTrue(surface.consumeAddedStep(2));
        assertTrue(surface.consumeModifiedStep(3));
        assertTrue(surface.consumeModifierHandledStep(4));

        assertFalse(surface.consumeAddedStep(2));
        assertFalse(surface.consumeModifiedStep(3));
        assertFalse(surface.consumeModifierHandledStep(4));
    }

    @Test
    void removingLastHeldStepClearsHeldState() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        surface.addHeldStep(2);
        assertTrue(surface.hasHeldSteps());
        assertTrue(surface.hasHeldStep(2));

        surface.removeHeldStep(2);

        assertFalse(surface.hasHeldSteps());
        assertFalse(surface.hasHeldStep(2));
    }

    @Test
    void resolvesModifierPressActionInControllerPriorityOrder() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        assertEquals(ChordStepPadSurface.ModifierPressAction.SELECT,
                surface.modifierPressAction(true, true, true, true));
        assertEquals(ChordStepPadSurface.ModifierPressAction.FIXED_LENGTH,
                surface.modifierPressAction(false, true, true, true));
        assertEquals(ChordStepPadSurface.ModifierPressAction.COPY,
                surface.modifierPressAction(false, false, true, true));
        assertEquals(ChordStepPadSurface.ModifierPressAction.DELETE,
                surface.modifierPressAction(false, false, false, true));
        assertEquals(ChordStepPadSurface.ModifierPressAction.NONE,
                surface.modifierPressAction(false, false, false, false));
    }

    @Test
    void resolvesRangePressActionFromHeldAnchor() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        assertEquals(ChordStepPadSurface.RangePressAction.NONE, surface.rangePressAction(3, true));

        surface.addHeldStep(2);
        surface.setHeldStepAnchor(2);

        assertEquals(ChordStepPadSurface.RangePressAction.EXTEND, surface.rangePressAction(5, true));
        assertEquals(ChordStepPadSurface.RangePressAction.BLOCK, surface.rangePressAction(5, false));
        assertEquals(ChordStepPadSurface.RangePressAction.NONE, surface.rangePressAction(2, true));
    }

    @Test
    void markingRangeExtendedAddsStepAndMarksBothStepsModified() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        surface.addHeldStep(2);
        surface.setHeldStepAnchor(2);

        surface.markRangeExtended(5);

        assertTrue(surface.hasHeldStep(5));
        assertTrue(surface.consumeModifiedStep(2));
        assertTrue(surface.consumeModifiedStep(5));
    }

    @Test
    void resolvesNormalStepPressActionAndTracksHeldStep() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        assertEquals(ChordStepPadSurface.StepPressAction.ADD_STEP,
                surface.stepPressAction(2, false, false));
        assertTrue(surface.hasHeldStep(2));
        assertEquals(2, surface.heldStepAnchor());

        assertEquals(ChordStepPadSurface.StepPressAction.LOAD_BUILDER,
                surface.stepPressAction(3, true, true));
        assertEquals(ChordStepPadSurface.StepPressAction.HOLD_EXISTING,
                surface.stepPressAction(4, true, false));
    }

    @Test
    void releaseActionPreservesAddedAndModifiedStepsButClearsPlainExistingSteps() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        surface.addHeldStep(2);
        surface.markAddedStep(2);
        assertEquals(ChordStepPadSurface.StepReleaseAction.NONE, surface.stepReleaseAction(2, true));

        surface.addHeldStep(3);
        surface.markModifiedStep(3);
        assertEquals(ChordStepPadSurface.StepReleaseAction.NONE, surface.stepReleaseAction(3, true));

        surface.addHeldStep(4);
        assertEquals(ChordStepPadSurface.StepReleaseAction.CLEAR_STEP, surface.stepReleaseAction(4, true));

        surface.addHeldStep(5);
        assertEquals(ChordStepPadSurface.StepReleaseAction.NONE, surface.stepReleaseAction(5, false));
    }

    @Test
    void workflowAddsAndKeepsNewStep() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final FakeStepCallbacks callbacks = new FakeStepCallbacks();
        callbacks.hasStepStartNote = false;

        surface.handleStepPadPress(2, true, 100, callbacks);

        assertEquals(List.of(2), callbacks.assignedSteps);
        assertTrue(surface.hasHeldStep(2));

        callbacks.hasStepStartNote = true;
        surface.handleStepPadPress(2, false, 0, callbacks);

        assertEquals(List.of(), callbacks.clearedSteps);
        assertFalse(surface.hasHeldStep(2));
    }

    @Test
    void workflowClearsPlainExistingStepOnRelease() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final FakeStepCallbacks callbacks = new FakeStepCallbacks();
        callbacks.hasStepStartNote = true;

        surface.handleStepPadPress(2, true, 100, callbacks);
        surface.handleStepPadPress(2, false, 0, callbacks);

        assertEquals(List.of(2), callbacks.clearedSteps);
    }

    @Test
    void workflowConsumesModifierPressOnRelease() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final FakeStepCallbacks callbacks = new FakeStepCallbacks();
        callbacks.selectHeld = true;

        surface.handleStepPadPress(2, true, 100, callbacks);
        surface.handleStepPadPress(2, false, 0, callbacks);

        assertEquals(List.of(2), callbacks.selectedSteps);
        assertEquals(List.of(), callbacks.clearedSteps);
    }

    @Test
    void workflowExtendsHeldRangeThroughCallbacks() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final FakeStepCallbacks callbacks = new FakeStepCallbacks();
        callbacks.hasStepStartNote = true;

        surface.handleStepPadPress(2, true, 100, callbacks);
        surface.handleStepPadPress(5, true, 100, callbacks);

        assertEquals(List.of("2-5"), callbacks.extendedRanges);
        assertTrue(surface.hasHeldStep(5));
        assertTrue(surface.consumeModifiedStep(2));
        assertTrue(surface.consumeModifiedStep(5));
    }

    private static NoteStep note(final int x, final int recurrenceLength, final int recurrenceMask) {
        final NoteStep note = mock(NoteStep.class);
        when(note.x()).thenReturn(x);
        when(note.recurrenceLength()).thenReturn(recurrenceLength);
        when(note.recurrenceMask()).thenReturn(recurrenceMask);
        return note;
    }

    private static final class FakeStepCallbacks implements ChordStepPadSurface.StepPadCallbacks {
        private boolean ensureSelected = true;
        private boolean accentGesture;
        private boolean selectHeld;
        private boolean fixedLengthHeld;
        private boolean copyHeld;
        private boolean deleteHeld;
        private boolean builderFamily;
        private boolean hasStepStartNote;
        private boolean canExtend = true;
        private final List<Integer> assignedSteps = new ArrayList<>();
        private final List<Integer> selectedSteps = new ArrayList<>();
        private final List<Integer> clearedSteps = new ArrayList<>();
        private final List<String> extendedRanges = new ArrayList<>();

        @Override
        public boolean ensureSelectedNoteClipSlot() {
            return ensureSelected;
        }

        @Override
        public boolean isAccentGestureActive() {
            return accentGesture;
        }

        @Override
        public void toggleAccentForStep(final int stepIndex) {
        }

        @Override
        public boolean isSelectHeld() {
            return selectHeld;
        }

        @Override
        public boolean isFixedLengthHeld() {
            return fixedLengthHeld;
        }

        @Override
        public boolean isCopyHeld() {
            return copyHeld;
        }

        @Override
        public boolean isDeleteHeld() {
            return deleteHeld;
        }

        @Override
        public void handleSelectStep(final int stepIndex) {
            selectedSteps.add(stepIndex);
        }

        @Override
        public void setLastStep(final int stepIndex) {
        }

        @Override
        public void pasteCurrentChordToStep(final int stepIndex) {
        }

        @Override
        public void clearChordStep(final int stepIndex) {
            clearedSteps.add(stepIndex);
        }

        @Override
        public boolean isBuilderFamily() {
            return builderFamily;
        }

        @Override
        public boolean canExtendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
            return canExtend;
        }

        @Override
        public boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
            extendedRanges.add(anchorStepIndex + "-" + targetStepIndex);
            return true;
        }

        @Override
        public void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {
        }

        @Override
        public void showBlockedStepInfo() {
        }

        @Override
        public boolean hasStepStartNote(final int stepIndex) {
            return hasStepStartNote;
        }

        @Override
        public boolean assignSelectedChordToStep(final int stepIndex, final int velocity) {
            assignedSteps.add(stepIndex);
            return true;
        }

        @Override
        public void loadBuilderFromStep(final int stepIndex) {
        }

        @Override
        public void showHeldStepInfo(final int stepIndex) {
        }

        @Override
        public void removeHeldBankFineStart(final int stepIndex) {
        }
    }
}
