package com.oikoaudio.fire.music;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedPitchContextControllerTest {

    @Test
    void initializesFromPreferences() {
        final SharedPitchContextController controller = new SharedPitchContextController(
                new SharedMusicalContext(MusicalScaleLibrary.getInstance()),
                MusicalScaleLibrary.getInstance());

        controller.initializeFromPreferences(
                FireControlPreferences.DEFAULT_SCALE_DORIAN,
                9,
                5);

        assertEquals(9, controller.getRootNote());
        assertEquals(5, controller.getOctave());
        assertEquals("Dorian", controller.getScaleDisplayName());
    }

    @Test
    void formatsShortScaleNamesForControllerDisplays() {
        final MusicalScaleLibrary library = MusicalScaleLibrary.getInstance();
        final SharedPitchContextController controller = new SharedPitchContextController(
                new SharedMusicalContext(library),
                library);

        controller.setScaleIndex(controller.findScaleIndex("Phrygian Dominant", 1));

        assertEquals("Phryg Dom", controller.getShortScaleDisplayName());
    }

    @Test
    void walksScaleDegreesFromSharedRootAndScale() {
        final MusicalScaleLibrary library = MusicalScaleLibrary.getInstance();
        final SharedPitchContextController controller = new SharedPitchContextController(
                new SharedMusicalContext(library),
                library);
        controller.setRootNote(0);
        controller.setScaleIndex(controller.findScaleIndex("Major", 1));

        assertTrue(controller.isRootMidiNote(0, 60));
        assertEquals(62, controller.nextScaleNote(60, 0));
        assertEquals(64, controller.transposeByScaleDegrees(60, 2));
    }
}
