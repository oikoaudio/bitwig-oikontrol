package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChordStepPadLightRendererTest {
    private static final int CLIP_ROW = 16;
    private static final int SOURCE_OFFSET = 16;
    private static final int STEP_OFFSET = 32;

    @Test
    void clipRowFallsBackToClipPadLightWhenNoRecurrenceRowIsVisible() {
        final Fixture fixture = new Fixture();

        assertSame(
                RgbLightState.GRAY_1,
                fixture.renderer.padLight(0, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
    }

    @Test
    void rendersSelectedBuilderSourcePadFromChordStepColors() {
        final Fixture fixture = new Fixture();
        fixture.builder.toggleNoteOffset(0);

        final RgbLightState expected = new RgbLightState(88, 18, 127, true);

        assertEquals(
                expected,
                fixture.renderer.padLight(SOURCE_OFFSET, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
    }

    @Test
    void rendersEverySelectedPresetPadAsSelected() {
        final Fixture fixture = new Fixture();
        fixture.selection.adjustFamily(1);
        fixture.selection.selectSlots(Set.of(0, 2), 2);
        final RgbLightState selected = new RgbLightState(110, 24, 118, true);

        assertEquals(
                selected,
                fixture.renderer.padLight(SOURCE_OFFSET, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
        assertEquals(
                selected,
                fixture.renderer.padLight(SOURCE_OFFSET + 2, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
    }

    @Test
    void rendersHeldStepAsChordStepHeldColor() {
        final Fixture fixture = new Fixture();
        fixture.surface.addHeldStep(0);
        fixture.ownedSteps.add(0);

        final RgbLightState expected = new RgbLightState(120, 88, 0, true).getBrightest();

        assertEquals(
                expected,
                fixture.renderer.padLight(STEP_OFFSET, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET));
    }

    @Test
    void heldPhysicalPadDoesNotRemainHighlightedAfterOwnershipTransfers() {
        final Fixture fixture = new Fixture();
        fixture.surface.addHeldStep(0);
        fixture.ownedSteps.add(1);

        final RgbLightState oldOwner =
                fixture.renderer.padLight(STEP_OFFSET, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET);
        final RgbLightState newOwner =
                fixture.renderer.padLight(STEP_OFFSET + 1, CLIP_ROW, SOURCE_OFFSET, STEP_OFFSET);

        assertEquals(RgbLightState.GRAY_1, oldOwner);
        assertEquals(RgbLightState.PURPLE, newOwner);
    }

    private static final class Fixture {
        private final ChordStepPadSurface surface = new ChordStepPadSurface();
        private final ChordStepChordSelection selection = new ChordStepChordSelection();
        private final Set<Integer> ownedSteps = new HashSet<>();
        private final ChordStepBuilderController builder;
        private final ChordStepPadLightRenderer renderer;

        private Fixture() {
            final MusicalScaleLibrary library = MusicalScaleLibrary.getInstance();
            final SharedPitchContextController pitchContext =
                    new SharedPitchContextController(new SharedMusicalContext(library), library);
            pitchContext.initializeFromPreferences(
                    FireControlPreferences.DEFAULT_SCALE_MAJOR, 0, 3);
            builder = new ChordStepBuilderController(selection, pitchContext, () -> 60, 16);
            renderer =
                    new ChordStepPadLightRenderer(
                            surface,
                            builder,
                            selection,
                            ignored -> RgbLightState.GRAY_1,
                            List::of,
                            () -> RgbLightState.PURPLE,
                            () -> 32,
                            () -> -1,
                            () -> -1,
                            ownedSteps::contains,
                            step -> false,
                            step -> false);
        }
    }
}
