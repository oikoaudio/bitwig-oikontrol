package com.oikoaudio.fire.music;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
