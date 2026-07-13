package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StepStyleEncoderInitializationTest {
    @Test
    void everyStepStyleModeCompletesLayoutBeforeConstructingItsPhysicalLayer() throws IOException {
        for (final String relativePath :
                List.of(
                        "sequence/DrumSequenceMode.java",
                        "melodic/MelodicStepMode.java",
                        "chordstep/ChordStepMode.java",
                        "nestedrhythm/NestedRhythmMode.java")) {
            final String source = source(relativePath);
            final int layoutAssignment = source.indexOf("encoderBankLayout =");
            final int layerConstruction = source.indexOf("new StepSequencerEncoderLayer");

            assertTrue(layoutAssignment >= 0, relativePath + " should assign encoderBankLayout");
            assertTrue(
                    layerConstruction > layoutAssignment,
                    relativePath + " should complete layout before constructing its layer");
            assertTrue(
                    source.substring(layerConstruction, layerConstruction + 120)
                            .contains("encoderBankLayout"),
                    relativePath + " should pass the completed layout explicitly");
            assertTrue(
                    source.contains("encoderLayer.activate()"),
                    relativePath + " should explicitly activate its physical layer");
            assertTrue(
                    source.contains("encoderLayer.deactivate()"),
                    relativePath + " should explicitly deactivate its physical layer");
        }
    }

    private static String source(final String relativePath) throws IOException {
        final Path relative = Path.of("src/main/java/com/oikoaudio/fire").resolve(relativePath);
        final Path source =
                Files.exists(relative) ? relative : Path.of("modules/akai-fire").resolve(relative);
        return Files.readString(source);
    }
}
