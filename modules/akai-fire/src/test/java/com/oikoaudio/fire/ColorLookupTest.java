package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bitwig.extension.api.Color;
import com.oikoaudio.fire.lights.RgbLightState;
import org.junit.jupiter.api.Test;

class ColorLookupTest {

    @Test
    void convertsBitwigColorUsingNormalizedRgbComponents() {
        final RgbLightState red = ColorLookup.getColor(Color.fromRGB255(255, 0, 0));

        assertEquals(new RgbLightState(127, 0, 0, true), red);
    }

    @Test
    void colorObjectAndObserverRgbPathMatch() {
        final RgbLightState fromColor = ColorLookup.getColor(Color.fromRGB255(64, 128, 255));
        final RgbLightState fromObserverValues = ColorLookup.getColor(64 / 255.0, 128 / 255.0, 1.0);

        assertEquals(fromObserverValues, fromColor);
    }
}
