package com.oikoaudio.fire;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedMusicalContextTest {

    @Test
    void normalizesRootAndOctave() {
        final SharedMusicalContext context = new SharedMusicalContext(MusicalScaleLibrary.getInstance());

        context.setRootNote(-1);
        context.setOctave(99);

        assertEquals(11, context.getRootNote());
        assertEquals(7, context.getOctave());
        assertEquals(95, context.getBaseMidiNote());
    }

    @Test
    void adjustsScaleIndexWithinBounds() {
        final SharedMusicalContext context = new SharedMusicalContext(MusicalScaleLibrary.getInstance());
        context.setScaleIndex(1);

        assertTrue(context.adjustScaleIndex(1, -1));
        assertEquals(2, context.getScaleIndex());
        assertFalse(context.adjustScaleIndex(-10, -1));
        assertEquals(2, context.getScaleIndex());
    }

    @Test
    void normalizesRemovedChromaticScaleToMajor() {
        final SharedMusicalContext context = new SharedMusicalContext(MusicalScaleLibrary.getInstance());

        context.setScaleIndex(-1);

        assertEquals(1, context.getScaleIndex());
        assertEquals("Major", context.getScaleDisplayName());
    }
}
