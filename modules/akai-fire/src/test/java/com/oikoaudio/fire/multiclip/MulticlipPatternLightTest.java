package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
import org.junit.jupiter.api.Test;

class MulticlipPatternLightTest {
    private final RgbLightState color = new RgbLightState(60, 90, 30, true);

    @Test
    void turnsStepsOutsideTheClipLoopOffEvenWhenTheyContainDormantNotes() {
        assertEquals(
                RgbLightState.OFF,
                MulticlipPatternLight.render(color, false, false, false, false, true, false));
    }

    @Test
    void distinguishesEmptyAndOccupiedStepsInsideTheClipLoop() {
        assertEquals(
                color.getVeryDimmed(),
                MulticlipPatternLight.render(color, true, false, false, false, false, false));
        assertEquals(
                color.getBrightend(),
                MulticlipPatternLight.render(color, true, false, false, false, true, false));
    }

    @Test
    void marksANonZeroPlayStartWithoutHidingTheMovingPlayhead() {
        assertEquals(
                StepPadLightHelper.renderClipStartIndicator(color),
                MulticlipPatternLight.render(color, true, false, false, false, false, true));
        assertEquals(
                StepPadLightHelper.renderClipStartIndicator(color),
                MulticlipPatternLight.render(color, true, false, false, false, true, true));
        assertEquals(
                RgbLightState.WHITE,
                MulticlipPatternLight.render(color, true, false, false, true, false, true));
    }
}
