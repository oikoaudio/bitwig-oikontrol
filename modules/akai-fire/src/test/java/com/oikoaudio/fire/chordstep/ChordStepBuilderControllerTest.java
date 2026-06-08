package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.music.SharedPitchContextController;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepBuilderControllerTest {
    @Test
    void mapsSourcePadsChromaticallyOrInKey() {
        final Fixture fixture = new Fixture();

        assertEquals(60, fixture.builder.noteMidiForPad(0));
        assertEquals(61, fixture.builder.noteMidiForPad(1));

        fixture.builder.toggleLayout();

        assertTrue(fixture.builder.isInKey());
        assertEquals("In Key", fixture.builder.layoutDisplayName());
        assertEquals(62, fixture.builder.noteMidiForPad(1));
    }

    @Test
    void latchOffSourcePadTapsReplaceBuilderNotesByDefault() {
        final Fixture fixture = new Fixture();

        assertFalse(fixture.builder.isLatchEnabled());

        assertTrue(fixture.builder.handleSourcePad(0, true));
        assertFalse(fixture.builder.handleSourcePad(0, false));
        assertTrue(fixture.builder.handleSourcePad(2, true));
        assertFalse(fixture.builder.handleSourcePad(2, false));

        assertArrayEquals(new int[] {62}, fixture.selection.renderSelectedChord(null, 0));
        assertFalse(fixture.selection.isBuilderNoteSelected(60));
        assertTrue(fixture.selection.isBuilderNoteSelected(62));
    }

    @Test
    void latchOffMultiPadGripPersistsAfterRelease() {
        final Fixture fixture = new Fixture();

        assertTrue(fixture.builder.handleSourcePad(0, true));
        assertTrue(fixture.builder.handleSourcePad(4, true));
        assertTrue(fixture.builder.handleSourcePad(7, true));
        assertFalse(fixture.builder.handleSourcePad(0, false));
        assertFalse(fixture.builder.handleSourcePad(4, false));
        assertFalse(fixture.builder.handleSourcePad(7, false));

        assertArrayEquals(new int[] {60, 64, 67}, fixture.selection.renderSelectedChord(null, 0));
    }

    @Test
    void latchOffRepeatedSourcePadPressRemainsActionable() {
        final Fixture fixture = new Fixture();

        assertTrue(fixture.builder.handleSourcePad(0, true));
        assertFalse(fixture.builder.handleSourcePad(0, false));

        assertTrue(fixture.builder.handleSourcePad(0, true));

        assertArrayEquals(new int[] {60}, fixture.selection.renderSelectedChord(null, 0));
    }

    @Test
    void latchOnTogglesBuilderNotesFromSourcePads() {
        final Fixture fixture = new Fixture();

        assertTrue(fixture.builder.setLatchEnabled(true));
        assertEquals("On", fixture.builder.latchDisplayName());

        assertTrue(fixture.builder.handleSourcePad(1, true));
        assertFalse(fixture.builder.handleSourcePad(1, false));

        assertTrue(fixture.selection.isBuilderNoteSelected(61));
        assertTrue(fixture.builder.isNoteSelectedForPad(1));

        assertTrue(fixture.builder.handleSourcePad(1, true));

        assertFalse(fixture.selection.isBuilderNoteSelected(61));
    }

    @Test
    void startsWithEmptyBuilderNotes() {
        final Fixture fixture = new Fixture();

        assertFalse(fixture.selection.hasBuilderNotes());
    }

    @Test
    void reportsPadRolesAgainstTheCurrentBuilderRoot() {
        final Fixture fixture = new Fixture();

        assertEquals(ChordStepBuilderController.PadRole.ROOT, fixture.builder.padRole(0));
        assertEquals(ChordStepBuilderController.PadRole.OUT_OF_SCALE, fixture.builder.padRole(1));

        fixture.builder.toggleLayout();

        assertEquals(ChordStepBuilderController.PadRole.IN_SCALE, fixture.builder.padRole(1));
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
