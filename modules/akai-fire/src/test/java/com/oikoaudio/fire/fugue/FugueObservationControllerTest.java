package com.oikoaudio.fire.fugue;

import com.bitwig.extension.controller.api.NoteStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FugueObservationControllerTest {
    @Test
    void observesAndRemovesNotesOnlyWhileActive() {
        final FugueObservationController observations = new FugueObservationController(256);
        final NoteStep note = note(1, 4, 60, NoteStep.State.NoteOn);

        observations.observe(note);
        assertTrue(observations.steps().isEmpty());

        observations.setActive(true);
        observations.observe(note);
        assertEquals(note, observations.steps().get(1).get(4).get(60));

        observations.observe(note(1, 4, 60, NoteStep.State.Empty));
        assertTrue(observations.steps().isEmpty());
    }

    @Test
    void refreshRebuildsOnlyTheSourceChannel() {
        final FugueObservationController observations = new FugueObservationController(2);
        observations.setActive(true);
        final NoteStep source = note(0, 1, 61, NoteStep.State.NoteOn);

        observations.refreshSource((step, pitch) -> step == 1 && pitch == 61
                ? source : note(0, step, pitch, NoteStep.State.Empty));

        assertEquals(source, observations.steps().get(0).get(1).get(61));
        assertFalse(observations.steps().containsKey(1));
    }

    @Test
    void clampsPlayingStepToTheObservedRange() {
        final FugueObservationController observations = new FugueObservationController(256);

        observations.updatePlayingStep(42);
        assertEquals(42, observations.playingStep());
        observations.updatePlayingStep(256);
        assertEquals(-1, observations.playingStep());
    }

    private static NoteStep note(final int channel, final int step, final int pitch, final NoteStep.State state) {
        final NoteStep note = mock(NoteStep.class);
        when(note.channel()).thenReturn(channel);
        when(note.x()).thenReturn(step);
        when(note.y()).thenReturn(pitch);
        when(note.state()).thenReturn(state);
        return note;
    }
}
