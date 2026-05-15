package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.music.SharedPitchContextController;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepBuilderControllerTest {
    @Test
    void mapsSourcePadsChromaticallyOrInKey() {
        final Fixture fixture = new Fixture();

        assertEquals(60, fixture.builder.noteMidiForPad(0));
        assertEquals(62, fixture.builder.noteMidiForPad(1));

        fixture.builder.toggleLayout();

        assertFalse(fixture.builder.isInKey());
        assertEquals("Chromatic", fixture.builder.layoutDisplayName());
        assertEquals(61, fixture.builder.noteMidiForPad(1));
    }

    @Test
    void togglesBuilderNotesFromSourcePads() {
        final Fixture fixture = new Fixture();

        fixture.builder.toggleNoteOffset(1);

        assertTrue(fixture.selection.isBuilderNoteSelected(62));
        assertTrue(fixture.builder.isNoteSelectedForPad(1));

        fixture.builder.toggleNoteOffset(1);

        assertFalse(fixture.selection.isBuilderNoteSelected(62));
    }

    @Test
    void seedsEmptyBuilderWithFirstVisibleRootNote() {
        final Fixture fixture = new Fixture();

        fixture.builder.ensureSeededIfEmpty();

        assertTrue(fixture.selection.isBuilderNoteSelected(60));
    }

    @Test
    void reportsPadRolesAgainstTheCurrentBuilderRoot() {
        final Fixture fixture = new Fixture();

        assertEquals(ChordStepBuilderController.PadRole.ROOT, fixture.builder.padRole(0));
        assertEquals(ChordStepBuilderController.PadRole.IN_SCALE, fixture.builder.padRole(1));

        fixture.builder.toggleLayout();

        assertEquals(ChordStepBuilderController.PadRole.OUT_OF_SCALE, fixture.builder.padRole(1));
    }

    private static final class Fixture {
        private final ChordStepChordSelection selection = new ChordStepChordSelection();
        private final SharedPitchContextController pitchContext;
        private final AtomicInteger firstVisibleNote = new AtomicInteger(60);
        private final ChordStepBuilderController builder;

        private Fixture() {
            final MusicalScaleLibrary library = MusicalScaleLibrary.getInstance();
            pitchContext = new SharedPitchContextController(new SharedMusicalContext(library), library);
            pitchContext.initializeFromPreferences(FireControlPreferences.DEFAULT_SCALE_MAJOR, 0, 3);
            builder = new ChordStepBuilderController(selection, pitchContext, firstVisibleNote::get, 16);
        }
    }
}
