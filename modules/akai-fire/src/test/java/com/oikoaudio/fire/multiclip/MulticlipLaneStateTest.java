package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MulticlipLaneStateTest {
    @Test
    void keepsStepsAndPlayheadsIndependentPerVisibleLane() {
        final MulticlipLaneState state = new MulticlipLaneState();

        state.setOccupied(0, 2, true);
        state.setOccupied(1, 7, true);
        state.setPlayingStep(0, 2);
        state.setPlayingStep(1, 7);

        assertTrue(state.isOccupied(0, 2));
        assertFalse(state.isOccupied(0, 7));
        assertTrue(state.isOccupied(1, 7));
        assertTrue(state.isPlaying(0, 2));
        assertFalse(state.isPlaying(1, 2));
        assertTrue(state.isPlaying(1, 7));
    }

    @Test
    void retargetClearsOnlyThatRowsObservedState() {
        final MulticlipLaneState state = new MulticlipLaneState();
        state.setOccupied(0, 3, true);
        state.setOccupied(1, 4, true);

        state.clearRow(0);

        assertFalse(state.isOccupied(0, 3));
        assertTrue(state.isOccupied(1, 4));
    }
}
