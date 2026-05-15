package com.oikoaudio.fire.nestedrhythm;

import com.oikoaudio.fire.lights.RgbLigthState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class NestedRhythmPadSurfaceTest {

    @Test
    void structureBinScalesFineStartAcrossVisibleStructurePads() {
        final NestedRhythmPadSurface surface = surface(new ArrayList<>(), 1680);

        assertEquals(0, surface.structureBinFor(0));
        assertEquals(16, surface.structureBinFor(840));
        assertEquals(31, surface.structureBinFor(1679));
        assertEquals(31, surface.structureBinFor(1680));
    }

    @Test
    void strongestPulseInBinUsesEffectiveVelocity() {
        final NestedRhythmEditablePulse quiet = pulse(0, 60, 60);
        final NestedRhythmEditablePulse loud = pulse(1, 70, 90);
        loud.velocityOffset = 20;
        final NestedRhythmPadSurface surface = surface(new ArrayList<>(List.of(quiet, loud)), 1680);

        assertSame(loud, surface.strongestPulseInBin(surface.structureBinFor(60)));
    }

    @Test
    void playingPulseIndexUsesEnabledPulseDuration() {
        final NestedRhythmEditablePulse disabled = pulse(0, 0, 120);
        disabled.enabled = false;
        final NestedRhythmEditablePulse active = pulse(1, 100, 120);
        final NestedRhythmPadSurface surface = surface(new ArrayList<>(List.of(disabled, active)), 1680);

        surface.handlePlayingStep(50);
        assertEquals(-1, surface.playingPulseIndex());

        surface.handlePlayingStep(110);
        assertEquals(1, surface.playingPulseIndex());
    }

    @Test
    void shiftedClipStartColumnOverlaysEmptyStructurePads() {
        final NestedRhythmPadSurface surface = surface(new ArrayList<>(), 1680, 0);

        assertEquals(new RgbLigthState(30, 0, 127, true),
                surface.getPadLight(NestedRhythmPadSurface.STRUCTURE_PAD_OFFSET));
    }

    private static NestedRhythmPadSurface surface(final List<NestedRhythmEditablePulse> pulses,
                                                 final int totalFineStepCount) {
        return surface(pulses, totalFineStepCount, -1);
    }

    private static NestedRhythmPadSurface surface(final List<NestedRhythmEditablePulse> pulses,
                                                 final int totalFineStepCount,
                                                 final int shiftedClipStartColumn) {
        return new NestedRhythmPadSurface(
                pulses,
                null,
                null,
                () -> false,
                () -> false,
                () -> totalFineStepCount,
                () -> shiftedClipStartColumn,
                ignored -> { },
                ignored -> RgbLigthState.OFF,
                () -> new RgbLigthState(0, 90, 34, true),
                (label, value) -> { });
    }

    private static NestedRhythmEditablePulse pulse(final int order, final int fineStart, final int velocity) {
        return new NestedRhythmEditablePulse(new NestedRhythmPattern.PulseEvent(
                order,
                fineStart,
                120,
                60,
                velocity,
                NestedRhythmPattern.Role.PRIMARY_ANCHOR,
                1.0));
    }
}
