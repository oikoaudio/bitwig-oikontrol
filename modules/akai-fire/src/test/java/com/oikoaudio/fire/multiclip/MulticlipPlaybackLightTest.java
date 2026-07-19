package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.RgbLightState;
import org.junit.jupiter.api.Test;

class MulticlipPlaybackLightTest {
    private final RgbLightState color = new RgbLightState(60, 90, 30, true);

    @Test
    void slowlyBlinksPlayingScenesLikeTheDrumXoxClipRow() {
        assertEquals(
                color.getBrightest(),
                MulticlipPlaybackLight.render(color, true, true, false, color, 0));
        assertEquals(color, MulticlipPlaybackLight.render(color, true, true, false, color, 4));
        assertEquals(
                color,
                MulticlipPlaybackLight.render(color, false, true, false, color.getSoftDimmed(), 0));
        assertEquals(
                color.getDimmed(),
                MulticlipPlaybackLight.render(color, false, true, false, color.getSoftDimmed(), 4));
    }

    @Test
    void quicklyBlinksQueuedScenesAndLeavesIdleScenesUntouched() {
        assertEquals(
                color.getBrightest(),
                MulticlipPlaybackLight.render(color, true, false, true, color, 0));
        assertEquals(
                color.getDimmed(),
                MulticlipPlaybackLight.render(color, true, false, true, color, 1));
        assertEquals(
                color.getSoftDimmed(),
                MulticlipPlaybackLight.render(
                        color, false, false, false, color.getSoftDimmed(), 0));
    }
}
