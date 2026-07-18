package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipEuclidStateTest {
    @Test
    void clampsTheFourDrumXoxEuclidControls() {
        MulticlipEuclidState state = MulticlipEuclidState.defaults();

        state = state.adjusted(0, -20).adjusted(1, 8).adjusted(2, -1).adjusted(3, 10);

        assertEquals(1, state.length());
        assertEquals(1, state.pulses());
        assertEquals(0, state.rotation());
        assertEquals(1, state.accentPulses());
    }

    @Test
    void buildsAndRotatesTheSameDeterministicEuclidShapePerClip() {
        final MulticlipEuclidState state = new MulticlipEuclidState(8, 3, 1, 0);

        assertArrayEquals(
                new boolean[] {false, true, false, false, true, false, false, true},
                state.pattern());
    }
}
