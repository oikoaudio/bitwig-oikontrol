package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.display.OledDisplay;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChordStepAccentControlsTest {
    @Test
    void latchesAccentModeOnUnmodifiedPressRelease() {
        final OledDisplay oled = mock(OledDisplay.class);
        final ChordStepAccentControls controls = new ChordStepAccentControls(oled);

        controls.handlePressed(true);
        controls.handlePressed(false);

        assertTrue(controls.isActive());
        verify(oled).valueInfo("Accent Mode", "Off");
        verify(oled).valueInfo("Accent Mode", "On");
    }

    @Test
    void modifiedHoldDoesNotToggleAccentModeOnRelease() {
        final OledDisplay oled = mock(OledDisplay.class);
        final ChordStepAccentControls controls = new ChordStepAccentControls(oled);

        controls.handlePressed(true);
        controls.markModified();
        controls.handlePressed(false);

        assertFalse(controls.isActive());
        verify(oled).clearScreenDelayed();
    }

    @Test
    void togglesNormalNotesToAccentVelocity() {
        final ChordStepAccentControls controls = controls();
        final NoteStep first = note(0.79, NoteStep.State.NoteOn);
        final NoteStep second = note(0.79, NoteStep.State.NoteOn);

        assertTrue(
                controls.toggleAccent(
                        List.of(first, second), ChordStepAccentControls.STANDARD_VELOCITY));

        verify(first).setVelocity(1.0);
        verify(second).setVelocity(1.0);
    }

    @Test
    void togglesAccentedNotesToStandardVelocity() {
        final ChordStepAccentControls controls = controls();
        final NoteStep note = note(1.0, NoteStep.State.NoteOn);

        assertFalse(
                controls.toggleAccent(List.of(note), ChordStepAccentControls.STANDARD_VELOCITY));

        verify(note).setVelocity(ChordStepAccentControls.STANDARD_VELOCITY / 127.0);
    }

    @Test
    void detectsAccentedOccupiedStep() {
        final ChordStepAccentControls controls = controls();
        final Map<Integer, NoteStep> notes = new LinkedHashMap<>();
        notes.put(60, note(1.0, NoteStep.State.NoteOn));

        assertTrue(controls.isStepAccented(notes, ChordStepAccentControls.STANDARD_VELOCITY));
    }

    private static ChordStepAccentControls controls() {
        return new ChordStepAccentControls(mock(OledDisplay.class));
    }

    private static NoteStep note(final double velocity, final NoteStep.State state) {
        final NoteStep note = mock(NoteStep.class);
        when(note.velocity()).thenReturn(velocity);
        when(note.state()).thenReturn(state);
        return note;
    }
}
