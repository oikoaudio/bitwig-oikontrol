package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.sequence.NoteVariationParameter;
import com.oikoaudio.fire.sequence.ObservedNoteVariationAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChordStepNoteVariationAdapterTest {

    @Test
    void appliesOneCoherentValueToEveryChordVoiceInTheFullLoop() {
        final ObservedNoteVariationAdapter adapter = new ObservedNoteVariationAdapter(64);
        final NoteStep root = note(32, 60, 0, NoteStep.State.NoteOn);
        final NoteStep third = note(32, 64, 2, NoteStep.State.NoteOn);
        final NoteStep later = note(48, 67, 0, NoteStep.State.NoteOn);
        adapter.handleObservedNote(root);
        adapter.handleObservedNote(third);
        adapter.handleObservedNote(later);

        final ObservedNoteVariationAdapter.Result result =
                adapter.apply(NoteVariationParameter.TIMBRE, 0.0, 0.8, 17L, 64);

        assertEquals(ObservedNoteVariationAdapter.Status.APPLIED, result.status());
        assertEquals(3, result.noteCount());
        final ArgumentCaptor<Double> rootValue = ArgumentCaptor.forClass(Double.class);
        final ArgumentCaptor<Double> thirdValue = ArgumentCaptor.forClass(Double.class);
        final ArgumentCaptor<Double> laterValue = ArgumentCaptor.forClass(Double.class);
        verify(root).setTimbre(rootValue.capture());
        verify(third).setTimbre(thirdValue.capture());
        verify(later).setTimbre(laterValue.capture());
        assertEquals(rootValue.getValue(), thirdValue.getValue());
        assertNotEquals(rootValue.getValue(), laterValue.getValue());
    }

    @Test
    void zeroAmountResetsChanceAndDisablesChancePlayback() {
        final ObservedNoteVariationAdapter adapter = new ObservedNoteVariationAdapter(64);
        final NoteStep note = note(8, 60, 0, NoteStep.State.NoteOn);
        adapter.handleObservedNote(note);

        final ObservedNoteVariationAdapter.Result result =
                adapter.apply(NoteVariationParameter.CHANCE, 1.0, 0.0, 5L, 16);

        assertEquals(ObservedNoteVariationAdapter.Status.RESET, result.status());
        verify(note).setChance(1.0);
        verify(note).setIsChanceEnabled(false);
    }

    @Test
    void refusesBeforeWritingWhenLoopExceedsObservationCapacity() {
        final ObservedNoteVariationAdapter adapter = new ObservedNoteVariationAdapter(16);
        final NoteStep note = note(8, 60, 0, NoteStep.State.NoteOn);
        adapter.handleObservedNote(note);

        final ObservedNoteVariationAdapter.Result result =
                adapter.apply(NoteVariationParameter.PRESSURE, 0.0, 0.5, 5L, 17);

        assertEquals(ObservedNoteVariationAdapter.Status.TOO_LARGE, result.status());
        verify(note, never()).setPressure(anyDouble());
    }

    @Test
    void ignoresSustainCellsAndNotesOutsideTheActiveLoop() {
        final ObservedNoteVariationAdapter adapter = new ObservedNoteVariationAdapter(64);
        final NoteStep inside = note(8, 60, 0, NoteStep.State.NoteOn);
        final NoteStep sustain = note(9, 60, 0, NoteStep.State.NoteSustain);
        final NoteStep outside = note(24, 64, 0, NoteStep.State.NoteOn);
        adapter.handleObservedNote(inside);
        adapter.handleObservedNote(sustain);
        adapter.handleObservedNote(outside);

        final ObservedNoteVariationAdapter.Result result =
                adapter.apply(NoteVariationParameter.PRESSURE, 0.0, 0.5, 5L, 16);

        assertEquals(1, result.noteCount());
        verify(inside).setPressure(anyDouble());
        verify(sustain, never()).setPressure(anyDouble());
        verify(outside, never()).setPressure(anyDouble());
    }

    private static NoteStep note(
            final int x, final int y, final int channel, final NoteStep.State state) {
        final NoteStep note = mock(NoteStep.class);
        when(note.x()).thenReturn(x);
        when(note.y()).thenReturn(y);
        when(note.channel()).thenReturn(channel);
        when(note.state()).thenReturn(state);
        return note;
    }
}
