package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

class MulticlipRowButtonRendererTest {
    @Test
    void rendersAudibleBrightMutedDimAndSoloedAmberWithoutSelectionFeedback() {
        assertEquals(
                BiColorLightState.OFF,
                MulticlipRowButtonRenderer.render(false, false, false, false));
        assertEquals(
                BiColorLightState.GREEN_FULL,
                MulticlipRowButtonRenderer.render(true, false, false, false));
        assertEquals(
                BiColorLightState.GREEN_FULL,
                MulticlipRowButtonRenderer.render(true, true, false, false));
        assertEquals(
                BiColorLightState.GREEN_HALF,
                MulticlipRowButtonRenderer.render(true, true, true, false));
        assertEquals(
                BiColorLightState.AMBER_FULL,
                MulticlipRowButtonRenderer.render(true, true, true, true));
    }
}
