package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChordStepEventIndexTest {
    @Test
    void tracksObservedFineGridStartsAsChordEvents() {
        final ChordStepEventIndex index = index();

        index.handleObservedStepData(33, 60, NoteStep.State.NoteOn.ordinal());
        index.handleObservedStepData(34, 60, NoteStep.State.NoteSustain.ordinal());

        final ChordStepEventIndex.Event event = index.eventForStep(2, Map.of());

        assertEquals(2, event.localStep());
        assertEquals(2, event.globalStep());
        assertEquals(33, event.anchorFineStart());
        assertEquals(60, event.notes().get(0).midiNote());
        assertEquals(2.0 / 64.0, event.notes().get(0).duration());
    }

    @Test
    void combinesVisibleNoteStepsWithObservedState() {
        final ChordStepEventIndex index = index();
        final NoteStep note = mock(NoteStep.class);
        when(note.x()).thenReturn(4);
        when(note.y()).thenReturn(64);
        when(note.state()).thenReturn(NoteStep.State.NoteOn);
        when(note.velocity()).thenReturn(100 / 127.0);
        when(note.duration()).thenReturn(0.5);

        index.handleNoteStepObject(note);

        assertTrue(index.hasStepStart(4));
        assertEquals(Set.of(4), index.visibleStartedSteps());
        assertEquals(64, index.eventForStep(4, Map.of()).notes().get(0).midiNote());
    }

    private static ChordStepEventIndex index() {
        return new ChordStepEventIndex(
                local -> local,
                global -> global,
                global -> global >= 0 && global < 32,
                ignored -> 96,
                16,
                0.25 / 16.0,
                0.25);
    }
}
