package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChordStepPadControlsTest {
    private static final int CLIP_ROW = 16;
    private static final int SOURCE_OFFSET = 16;
    private static final int STEP_OFFSET = 32;

    @Test
    void delegatesPadPressesToController() {
        final ChordStepPadControllerTest.FakeHost host = new ChordStepPadControllerTest.FakeHost();
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        final ChordStepPadControls controls = controls(surface, host);

        controls.handlePadPress(3, true, 100);

        assertEquals(List.of("clip 3 true"), host.events);
    }

    @Test
    void delegatesPadLightsToRenderer() {
        final ChordStepPadSurface surface = new ChordStepPadSurface();
        surface.addHeldStep(0);
        final ChordStepPadControls controls = controls(surface, new ChordStepPadControllerTest.FakeHost());

        final RgbLigthState expected = new RgbLigthState(120, 88, 0, true).getBrightest();

        assertEquals(expected, controls.padLight(STEP_OFFSET));
    }

    private static ChordStepPadControls controls(final ChordStepPadSurface surface,
                                                final ChordStepPadController.Host host) {
        final ChordStepPadController controller =
                new ChordStepPadController(surface, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET, host);
        final MusicalScaleLibrary library = MusicalScaleLibrary.getInstance();
        final SharedPitchContextController pitchContext =
                new SharedPitchContextController(new SharedMusicalContext(library), library);
        pitchContext.initializeFromPreferences(FireControlPreferences.DEFAULT_SCALE_MAJOR, 0, 3);
        final ChordStepChordSelection selection = new ChordStepChordSelection();
        final ChordStepBuilderController builder =
                new ChordStepBuilderController(selection, pitchContext, () -> 60, 16);
        final ChordStepPadLightRenderer renderer = new ChordStepPadLightRenderer(
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
        return new ChordStepPadControls(controller, renderer, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET);
    }
}
