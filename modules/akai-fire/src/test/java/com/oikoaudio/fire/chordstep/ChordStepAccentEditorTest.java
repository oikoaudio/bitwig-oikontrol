package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChordStepAccentEditorTest {
    @Test
    void togglesAllNormalNotesToAccentVelocity() {
        final ChordStepAccentEditor editor = new ChordStepAccentEditor();
        final NoteStep first = note(0.79, NoteStep.State.NoteOn);
        final NoteStep second = note(0.79, NoteStep.State.NoteOn);

        assertTrue(editor.toggleAccent(List.of(first, second), ChordStepAccentEditor.STANDARD_VELOCITY));

        verify(first).setVelocity(1.0);
        verify(second).setVelocity(1.0);
    }

    @Test
    void togglesAllAccentedNotesToStandardVelocity() {
        final ChordStepAccentEditor editor = new ChordStepAccentEditor();
        final NoteStep note = note(1.0, NoteStep.State.NoteOn);

        assertFalse(editor.toggleAccent(List.of(note), ChordStepAccentEditor.STANDARD_VELOCITY));

        verify(note).setVelocity(ChordStepAccentEditor.STANDARD_VELOCITY / 127.0);
    }

    @Test
    void detectsAccentedOccupiedStep() {
        final ChordStepAccentEditor editor = new ChordStepAccentEditor();
        final Map<Integer, NoteStep> notes = new LinkedHashMap<>();
        notes.put(60, note(1.0, NoteStep.State.NoteOn));

        assertTrue(editor.isStepAccented(notes, ChordStepAccentEditor.STANDARD_VELOCITY));
    }

    private static NoteStep note(final double velocity, final NoteStep.State state) {
        final NoteStep note = mock(NoteStep.class);
        when(note.velocity()).thenReturn(velocity);
        when(note.state()).thenReturn(state);
        return note;
    }
}
