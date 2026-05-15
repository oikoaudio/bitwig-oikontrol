package com.oikoaudio.fire.nestedrhythm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedRhythmEditablePatternTest {

    @Test
    void preservesLocalEditsForConfidentOverlayMatch() {
        final NestedRhythmEditablePattern editable = new NestedRhythmEditablePattern();
        editable.applyGeneratedPattern(pattern(event(0, 100, 60, NestedRhythmPattern.Role.PRIMARY_ANCHOR)),
                settings(), 4096);

        final NestedRhythmEditablePulse edited = editable.pulses().get(0);
        edited.velocityOffset = 12;
        edited.gateScale = 0.65;
        edited.pressureOffset = 0.25;
        edited.recurrenceLength = 4;
        edited.recurrenceMask = 0b0101;
        edited.recurrenceEdited = true;
        edited.enabled = false;

        editable.applyGeneratedPattern(pattern(event(1, 112, 60, NestedRhythmPattern.Role.PRIMARY_ANCHOR)),
                settings(), 4096);

        final NestedRhythmEditablePulse restored = editable.pulses().get(0);
        assertEquals(12, restored.velocityOffset);
        assertEquals(0.65, restored.gateScale);
        assertEquals(0.25, restored.pressureOffset);
        assertEquals(4, restored.recurrenceLength);
        assertEquals(0b0101, restored.recurrenceMask);
        assertTrue(restored.recurrenceEdited);
        assertFalse(restored.enabled);
    }

    @Test
    void dropsLocalEditsWhenRoleNoLongerMatches() {
        final NestedRhythmEditablePattern editable = new NestedRhythmEditablePattern();
        editable.applyGeneratedPattern(pattern(event(0, 100, 60, NestedRhythmPattern.Role.PRIMARY_ANCHOR)),
                settings(), 4096);
        editable.pulses().get(0).enabled = false;
        editable.pulses().get(0).velocityOffset = 12;

        editable.applyGeneratedPattern(pattern(event(0, 100, 60, NestedRhythmPattern.Role.RATCHET_LEAD)),
                settings(), 4096);

        final NestedRhythmEditablePulse current = editable.pulses().get(0);
        assertTrue(current.enabled);
        assertEquals(0, current.velocityOffset);
    }

    @Test
    void resetEditsRestoresNeutralLocalOverlay() {
        final NestedRhythmEditablePattern editable = new NestedRhythmEditablePattern();
        editable.applyGeneratedPattern(pattern(event(0, 100, 60, NestedRhythmPattern.Role.PRIMARY_ANCHOR)),
                settings(), 4096);
        final NestedRhythmEditablePulse pulse = editable.pulses().get(0);
        pulse.velocityOffset = -12;
        pulse.gateScale = 1.4;
        pulse.chanceOffset = -0.5;
        pulse.recurrenceEdited = true;
        pulse.enabled = false;

        editable.resetEdits();

        assertEquals(0, pulse.velocityOffset);
        assertEquals(1.0, pulse.gateScale);
        assertEquals(0.0, pulse.chanceOffset);
        assertFalse(pulse.recurrenceEdited);
        assertTrue(pulse.enabled);
    }

    @Test
    void pulseContainmentWrapsAroundClipEnd() {
        final NestedRhythmEditablePulse pulse = new NestedRhythmEditablePulse(
                new NestedRhythmPattern.PulseEvent(
                        0, 14, 4, 60, 100, NestedRhythmPattern.Role.PRIMARY_ANCHOR, 1.0));

        assertTrue(pulse.containsFineStep(14, 16));
        assertTrue(pulse.containsFineStep(1, 16));
        assertFalse(pulse.containsFineStep(5, 16));
    }

    private static NestedRhythmPattern pattern(final NestedRhythmPattern.PulseEvent event) {
        return new NestedRhythmPattern(List.of(event));
    }

    private static NestedRhythmPattern.PulseEvent event(final int order,
                                                       final int fineStart,
                                                       final int midiNote,
                                                       final NestedRhythmPattern.Role role) {
        return new NestedRhythmPattern.PulseEvent(order, fineStart, 120, midiNote, 96, role, 0.75);
    }

    private static NestedRhythmExpressionSettings settings() {
        return new NestedRhythmExpressionSettings(
                0.0,
                0.0,
                0,
                0.0,
                0.0,
                0,
                0.0,
                0.0,
                0,
                1.0,
                1.0,
                0,
                0.0,
                1.0 / NestedRhythmGenerator.FINE_STEPS_PER_QUARTER);
    }
}
