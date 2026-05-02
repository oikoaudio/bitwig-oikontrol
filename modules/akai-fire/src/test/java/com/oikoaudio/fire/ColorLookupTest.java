package com.oikoaudio.fire;

import com.bitwig.extension.api.Color;
import com.oikoaudio.fire.lights.RgbLigthState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorLookupTest {

    @Test
    void convertsBitwigColorUsingNormalizedRgbComponents() {
        final RgbLigthState red = ColorLookup.getColor(Color.fromRGB255(255, 0, 0));

        assertEquals(new RgbLigthState(127, 0, 0, true), red);
    }

    @Test
    void colorObjectAndObserverRgbPathMatch() {
        final RgbLigthState fromColor = ColorLookup.getColor(Color.fromRGB255(64, 128, 255));
        final RgbLigthState fromObserverValues = ColorLookup.getColor(64 / 255.0, 128 / 255.0, 1.0);

        assertEquals(fromObserverValues, fromColor);
    }
}
