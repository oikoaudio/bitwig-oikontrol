package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

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

    @Test
    void fineGridOwnershipOverridesTheCoarseVisibleNoteBucket() {
        final ChordStepEventIndex index = index();
        final NoteStep coarseNote = mock(NoteStep.class);
        when(coarseNote.x()).thenReturn(2);
        when(coarseNote.y()).thenReturn(60);
        when(coarseNote.state()).thenReturn(NoteStep.State.NoteOn);

        index.handleNoteStepObject(coarseNote);
        index.handleObservedStepData(40, 60, NoteStep.State.NoteOn.ordinal());

        assertFalse(index.hasStepStart(2));
        assertTrue(index.hasStepStart(3));
        assertEquals(Set.of(3), index.visibleStartedSteps());
        assertEquals(Set.of(), index.notesAtStep(2));
        assertEquals(Set.of(60), index.notesAtStep(3));
    }

    @Test
    void appliesCapturedProbabilityExpressionAndConditionsToAReplacementPitch() {
        final ChordStepEventIndex index = index();
        final NoteStep source = mock(NoteStep.class);
        when(source.chance()).thenReturn(0.63);
        when(source.isChanceEnabled()).thenReturn(true);
        when(source.pressure()).thenReturn(0.41);
        when(source.timbre()).thenReturn(-0.22);
        when(source.velocitySpread()).thenReturn(0.18);
        when(source.recurrenceLength()).thenReturn(5);
        when(source.recurrenceMask()).thenReturn(0b10101);
        when(source.isRecurrenceEnabled()).thenReturn(true);
        when(source.occurrence()).thenReturn(NoteOccurrence.FILL);
        when(source.isOccurrenceEnabled()).thenReturn(true);
        final NoteStep replacement = mock(NoteStep.class);
        when(replacement.x()).thenReturn(4);
        when(replacement.y()).thenReturn(67);
        when(replacement.state()).thenReturn(NoteStep.State.NoteOn);

        index.addPendingNoteSnapshot(4, 67, source);
        index.handleNoteStepObject(replacement);

        verify(replacement).setChance(0.63);
        verify(replacement).setIsChanceEnabled(true);
        verify(replacement).setPressure(0.41);
        verify(replacement).setTimbre(-0.22);
        verify(replacement).setVelocitySpread(0.18);
        verify(replacement).setRecurrence(5, 0b10101);
        verify(replacement).setIsRecurrenceEnabled(true);
        verify(replacement).setOccurrence(NoteOccurrence.FILL);
        verify(replacement).setIsOccurrenceEnabled(true);
    }

    @Test
    void appliesCapturedInsertionDefaultsWhenANewPitchBecomesObservable() {
        final ChordStepEventIndex index = index();
        final ChordStepInsertionDefaults defaults = new ChordStepInsertionDefaults(100, 0.25);
        defaults.adjust(com.oikoaudio.fire.sequence.NoteStepAccess.PRESSURE, 12);
        defaults.adjust(com.oikoaudio.fire.sequence.NoteStepAccess.CHANCE, -3);
        final NoteStep inserted = mock(NoteStep.class);
        when(inserted.x()).thenReturn(7);
        when(inserted.y()).thenReturn(72);
        when(inserted.state()).thenReturn(NoteStep.State.NoteOn);

        index.addPendingInsertionDefaults(7, 72, defaults.snapshot());
        index.handleNoteStepObject(inserted);

        verify(inserted).setPressure(0.12);
        verify(inserted).setChance(0.85);
        verify(inserted).setIsChanceEnabled(true);
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
