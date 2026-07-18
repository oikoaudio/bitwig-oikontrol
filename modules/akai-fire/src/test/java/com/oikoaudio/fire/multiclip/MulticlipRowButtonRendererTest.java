package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

class MulticlipRowButtonRendererTest {
    @Test
    void rendersNonexistentNormalActiveMutedAndSoloedPriority() {
        assertEquals(
                BiColorLightState.OFF,
                MulticlipRowButtonRenderer.render(false, false, false, false));
        assertEquals(
                BiColorLightState.HALF,
                MulticlipRowButtonRenderer.render(true, false, false, false));
        assertEquals(
                BiColorLightState.AMBER_HALF,
                MulticlipRowButtonRenderer.render(true, true, false, false));
        assertEquals(
                BiColorLightState.GREEN_FULL,
                MulticlipRowButtonRenderer.render(true, true, true, false));
        assertEquals(
                BiColorLightState.AMBER_FULL,
                MulticlipRowButtonRenderer.render(true, true, true, true));
    }
}
