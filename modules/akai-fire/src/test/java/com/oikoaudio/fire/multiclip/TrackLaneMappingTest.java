package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TrackLaneMappingTest {
    @Test
    void mapsDirectChildPositionToLaneChannelNotePageAndRow() {
        final TrackLaneMapping first = TrackLaneMapping.fromChildPosition(0);
        assertEquals(1, first.laneNumber());
        assertEquals(0, first.midiChannel());
        assertEquals(36, first.midiNote());
        assertEquals(0, first.page());
        assertEquals(0, first.row());

        final TrackLaneMapping last = TrackLaneMapping.fromChildPosition(15);
        assertEquals(16, last.laneNumber());
        assertEquals(15, last.midiChannel());
        assertEquals(51, last.midiNote());
        assertEquals(3, last.page());
        assertEquals(3, last.row());
    }

    @Test
    void rejectsPositionsOutsideTheSixteenLaneContract() {
        assertThrows(IllegalArgumentException.class, () -> TrackLaneMapping.fromChildPosition(-1));
        assertThrows(IllegalArgumentException.class, () -> TrackLaneMapping.fromChildPosition(16));
    }
}
