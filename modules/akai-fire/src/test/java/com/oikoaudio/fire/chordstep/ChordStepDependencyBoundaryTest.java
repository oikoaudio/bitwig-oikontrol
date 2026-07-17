package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChordStepDependencyBoundaryTest {
    @Test
    void modeLocalBindingAndEncoderOwnersDoNotDependOnTheCompleteExtension() throws IOException {
        assertFalse(source("ChordStepControlBindings.java").contains("AkaiFireOikontrolExtension"));
        assertFalse(source("ChordStepEncoderControls.java").contains("AkaiFireOikontrolExtension"));
    }

    private static String source(final String fileName) throws IOException {
        final Path relative =
                Path.of("src/main/java/com/oikoaudio/fire/chordstep").resolve(fileName);
        final Path source =
                Files.exists(relative) ? relative : Path.of("modules/akai-fire").resolve(relative);
        return Files.readString(source);
    }
}
