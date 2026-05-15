package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.note.LivePadSurfaceLayer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ChordStepModeDecouplingTest {
    @Test
    void chordStepModeDoesNotUsePitchedSurfaceInheritance() {
        assertFalse(LivePadSurfaceLayer.class.isAssignableFrom(ChordStepMode.class));
        for (final Class<?> nestedClass : ChordStepMode.class.getDeclaredClasses()) {
            assertFalse(LivePadSurfaceLayer.class.isAssignableFrom(nestedClass));
        }
    }

    @Test
    void livePadSurfaceLayerDoesNotRetainChordStepImplementation() throws IOException {
        final Path sourcePath = Path.of("src/main/java/com/oikoaudio/fire/note/LivePadSurfaceLayer.java");
        final String source = Files.readString(Files.exists(sourcePath)
                ? sourcePath
                : Path.of("modules/akai-fire").resolve(sourcePath));

        assertFalse(source.contains("com.oikoaudio.fire.chordstep"));
        assertFalse(source.contains("ChordStep"));
        assertFalse(source.contains("NoteStepSubMode"));
    }
}
