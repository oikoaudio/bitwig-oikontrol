package com.oikoaudio.fire.note;

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

        assertSame(held.getBrightest(), surface.stepPadLight(5, 16,
                true, false, false, true, 0,
                RgbLigthState.PURPLE, RgbLigthState.GRAY_1, held));
    }

    @Test
    void rendersOccupiedAccentedStepBright() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final RgbLigthState occupied = RgbLigthState.PURPLE;

        assertSame(occupied.getBrightend(), surface.stepPadLight(5, 16,
                true, true, false, false, 0,
                occupied, RgbLigthState.GRAY_1, new RgbLigthState(120, 88, 0, true)));
    }

    @Test
    void rendersOutsideLoopAsOff() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();

        assertSame(RgbLigthState.OFF, surface.stepPadLight(17, 16,
                true, true, false, false, 0,
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

    private static NoteStep note(final int x, final int recurrenceLength, final int recurrenceMask) {
        final NoteStep note = mock(NoteStep.class);
        when(note.x()).thenReturn(x);
        when(note.recurrenceLength()).thenReturn(recurrenceLength);
        when(note.recurrenceMask()).thenReturn(recurrenceMask);
        return note;
    }
}
