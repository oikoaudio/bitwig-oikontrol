package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepVisibleClipCacheTest {
    @Test
    void tracksVisibleClipNotesByStep() {
        final ChordStepVisibleClipCache cache = new ChordStepVisibleClipCache(32);

        cache.handleStepData(4, 60, NoteStep.State.NoteOn.ordinal());
        cache.handleStepData(4, 64, NoteStep.State.NoteOn.ordinal());

        assertTrue(cache.hasStepContent(4));
        assertEquals(Set.of(60, 64), cache.notesAtStep(4));
    }

    @Test
    void removesStepWhenLastVisibleNoteBecomesEmpty() {
        final ChordStepVisibleClipCache cache = new ChordStepVisibleClipCache(32);

        cache.handleStepData(4, 60, NoteStep.State.NoteOn.ordinal());
        cache.handleStepData(4, 60, NoteStep.State.Empty.ordinal());

        assertFalse(cache.hasStepContent(4));
        assertEquals(Set.of(), cache.notesAtStep(4));
    }

    @Test
    void ignoresStepDataOutsideVisibleRange() {
        final ChordStepVisibleClipCache cache = new ChordStepVisibleClipCache(32);

        cache.handleStepData(-1, 60, NoteStep.State.NoteOn.ordinal());
        cache.handleStepData(32, 60, NoteStep.State.NoteOn.ordinal());

        assertFalse(cache.hasStepContent(-1));
        assertFalse(cache.hasStepContent(32));
    }

    @Test
    void returnsDefensiveNoteSnapshots() {
        final ChordStepVisibleClipCache cache = new ChordStepVisibleClipCache(32);
        cache.handleStepData(4, 60, NoteStep.State.NoteOn.ordinal());

        final Set<Integer> notes = cache.notesAtStep(4);
        notes.add(64);

        assertEquals(Set.of(60), cache.notesAtStep(4));
    }
}
