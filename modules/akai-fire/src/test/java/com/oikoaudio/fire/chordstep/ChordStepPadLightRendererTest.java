package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChordStepPadLightRendererTest {
    private static final int CLIP_ROW = 16;
    private static final int SOURCE_OFFSET = 16;
    private static final int STEP_OFFSET = 32;

    @Test
    void clipRowFallsBackToClipPadLightWhenNoRecurrenceRowIsVisible() {
        final Fixture fixture = new Fixture();

        assertSame(RgbLigthState.GRAY_1, fixture.renderer.padLight(0, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
    }

    @Test
    void rendersSelectedBuilderSourcePadFromChordStepColors() {
        final Fixture fixture = new Fixture();
        fixture.builder.toggleNoteOffset(0);

        final RgbLigthState expected = new RgbLigthState(88, 18, 127, true);

        assertEquals(expected, fixture.renderer.padLight(SOURCE_OFFSET, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
    }

    @Test
    void rendersHeldStepAsChordStepHeldColor() {
        final Fixture fixture = new Fixture();
        fixture.surface.addHeldStep(0);

        final RgbLigthState expected = new RgbLigthState(120, 88, 0, true).getBrightest();

        assertEquals(expected, fixture.renderer.padLight(STEP_OFFSET, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
    }

    private static final class Fixture {
        private final ChordStepPadSurface surface = new ChordStepPadSurface();
        private final ChordStepChordSelection selection = new ChordStepChordSelection();
        private final ChordStepBuilderController builder;
        private final ChordStepPadLightRenderer renderer;

        private Fixture() {
            final MusicalScaleLibrary library = MusicalScaleLibrary.getInstance();
            final SharedPitchContextController pitchContext =
                    new SharedPitchContextController(new SharedMusicalContext(library), library);
            pitchContext.initializeFromPreferences(FireControlPreferences.DEFAULT_SCALE_MAJOR, 0, 3);
            builder = new ChordStepBuilderController(selection, pitchContext, () -> 60, 16);
            renderer = new ChordStepPadLightRenderer(
                    surface,
                    builder,
                    selection,
                    ignored -> RgbLigthState.GRAY_1,
                    List::of,
                    () -> RgbLigthState.PURPLE,
                    () -> 32,
                    () -> -1,
                    () -> -1,
                    step -> false,
                    step -> false,
                    step -> false);
        }
    }
}
