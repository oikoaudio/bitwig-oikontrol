package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class MulticlipLaneStateTest {
    @Test
    void observationsReceivedWhileRetargetSettlesRemainVisibleWhenEditingBecomesReady() {
        final MulticlipLaneState state = new MulticlipLaneState();

        state.beginRetarget(1);
        assertFalse(state.isReady(1));
        assertTrue(state.acceptsObservations(1));

        state.observeChannel(1, 4, 7, true);
        state.finishRetarget(1);

        assertTrue(state.isReady(1));
        assertTrue(state.isOccupied(1, 4));
        assertEquals(Set.of(7), state.channelsAt(1, 4));
    }

    @Test
    void deactivatedRowsRejectLateObservationsFromTheirPreviousTarget() {
        final MulticlipLaneState state = new MulticlipLaneState();

        state.beginRetarget(2);
        state.deactivateRow(2);

        assertFalse(state.isReady(2));
        assertFalse(state.acceptsObservations(2));
    }

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

    @Test
    void tracksUnexpectedObservedChannelsWithoutChangingLaneIdentity() {
        final MulticlipLaneState state = new MulticlipLaneState();

        state.observeChannel(2, 6, 11, true);
        state.observeChannel(2, 6, 2, true);
        assertTrue(state.isOccupied(2, 6));
        assertEquals(java.util.Set.of(2, 11), state.channelsAt(2, 6));

        state.observeChannel(2, 6, 2, false);
        assertTrue(state.isOccupied(2, 6));
        state.observeChannel(2, 6, 11, false);
        assertFalse(state.isOccupied(2, 6));
    }
}
