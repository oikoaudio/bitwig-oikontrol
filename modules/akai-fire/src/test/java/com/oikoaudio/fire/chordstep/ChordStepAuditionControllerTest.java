package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.values.Midi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChordStepAuditionControllerTest {
    @Test
    void startsAndStopsAuditionedNotes() {
        final NoteInput noteInput = mock(NoteInput.class);
        final ChordStepAuditionController controller = new ChordStepAuditionController(noteInput);

        controller.startAudition(new int[]{60, 64}, 90);
        controller.stopAudition();

        verify(noteInput).sendRawMidiEvent(Midi.NOTE_ON, 60, 90);
        verify(noteInput).sendRawMidiEvent(Midi.NOTE_ON, 64, 90);
        verify(noteInput).sendRawMidiEvent(Midi.NOTE_OFF, 60, 0);
        verify(noteInput).sendRawMidiEvent(Midi.NOTE_OFF, 64, 0);
    }

    @Test
    void startingNewAuditionStopsPreviousNotes() {
        final NoteInput noteInput = mock(NoteInput.class);
        final ChordStepAuditionController controller = new ChordStepAuditionController(noteInput);

        controller.startAudition(new int[]{60}, 90);
        controller.startAudition(new int[]{67}, 80);

        verify(noteInput).sendRawMidiEvent(Midi.NOTE_ON, 60, 90);
        verify(noteInput).sendRawMidiEvent(Midi.NOTE_OFF, 60, 0);
        verify(noteInput).sendRawMidiEvent(Midi.NOTE_ON, 67, 80);
    }

    @Test
    void configuresPolyAftertouchAsTimbreExpression() {
        final NoteInput noteInput = mock(NoteInput.class);
        final ChordStepAuditionController controller = new ChordStepAuditionController(noteInput);

        controller.configureExpression();

        verify(noteInput).assignPolyphonicAftertouchToExpression(0, NoteInput.NoteExpression.TIMBRE_UP, 1);
    }

    @Test
    void clearsKeyTranslationForAllNotes() {
        final NoteInput noteInput = mock(NoteInput.class);
        final ChordStepAuditionController controller = new ChordStepAuditionController(noteInput);
        final ArgumentCaptor<Integer[]> captor = ArgumentCaptor.forClass(Integer[].class);

        controller.clearTranslation();

        verify(noteInput).setKeyTranslationTable(captor.capture());
        assertTrue(Arrays.stream(captor.getValue()).allMatch(value -> value == -1));
    }
}
