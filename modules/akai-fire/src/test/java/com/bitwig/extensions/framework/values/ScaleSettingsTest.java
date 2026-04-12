package com.bitwig.extensions.framework.values;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScaleSettingsTest {

    @Test
    void octaveModificationStaysWithinBounds() {
        final ScaleSettings settings = new ScaleSettings();

        settings.modifyOctave(-1);
        assertEquals(3, settings.getOctaveOffset().get());

        for (int i = 0; i < 10; i++) {
            settings.modifyOctave(1);
        }
        assertEquals(6, settings.getOctaveOffset().get());

        for (int i = 0; i < 10; i++) {
            settings.modifyOctave(-1);
        }
        assertEquals(0, settings.getOctaveOffset().get());
    }

    @Test
    void scaleAndLayoutObjectsIncrementUsingConfiguredConverters() {
        final ScaleSettings settings = new ScaleSettings();

        assertEquals("Chromatic", settings.getScale().displayedValue());
        settings.getScale().increment(1);
        assertEquals(Scale.MAJOR, settings.getScale().get());
        assertEquals("Ionian/Major", settings.getScale().displayedValue());

        assertEquals("Isomorphic", settings.getLayoutType().displayedValue());
        settings.getLayoutType().increment(1);
        assertEquals(KeyboardLayoutType.IMITATE_KEYS, settings.getLayoutType().get());
        assertEquals("Piano Layout", settings.getLayoutType().displayedValue());
    }
}
