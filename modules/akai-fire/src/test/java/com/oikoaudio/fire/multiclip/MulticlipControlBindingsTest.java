package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.control.TrackSelectIndicatorLights;
import org.junit.jupiter.api.Test;

class MulticlipControlBindingsTest {
    @Test
    void usesSequencerStatusLedColorsForTheFourMuteButtonFunctions() {
        for (int row = 0; row < 3; row++) {
            assertEquals(
                    TrackSelectIndicatorLights.green(false),
                    MulticlipControlBindings.editStatusLightState(row, false));
            assertEquals(
                    TrackSelectIndicatorLights.green(true),
                    MulticlipControlBindings.editStatusLightState(row, true));
        }
        assertEquals(
                TrackSelectIndicatorLights.red(false),
                MulticlipControlBindings.editStatusLightState(3, false));
        assertEquals(
                TrackSelectIndicatorLights.red(true),
                MulticlipControlBindings.editStatusLightState(3, true));
    }
}
