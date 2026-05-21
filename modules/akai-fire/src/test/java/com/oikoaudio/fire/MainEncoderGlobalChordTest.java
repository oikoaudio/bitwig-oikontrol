package com.oikoaudio.fire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainEncoderGlobalChordTest {
    @Test
    void shiftTurnMovesPlaybackStartByGrid() {
        assertEquals(MainEncoderGlobalChord.Action.PLAYBACK_START_GRID,
                MainEncoderGlobalChord.resolve(1, false, false, true, false));
    }

    @Test
    void patternTurnMovesBetweenCueMarkers() {
        assertEquals(MainEncoderGlobalChord.Action.CUE_MARKER,
                MainEncoderGlobalChord.resolve(1, false, true, false, false));
    }

    @Test
    void shiftPatternTurnFineMovesPlaybackStart() {
        assertEquals(MainEncoderGlobalChord.Action.PLAYBACK_START_FINE,
                MainEncoderGlobalChord.resolve(1, false, true, true, false));
    }

    @Test
    void altChordKeepsExistingTimelineZoomBehavior() {
        assertEquals(MainEncoderGlobalChord.Action.TIMELINE_ZOOM_HORIZONTAL,
                MainEncoderGlobalChord.resolve(1, false, false, false, true));
        assertEquals(MainEncoderGlobalChord.Action.TIMELINE_ZOOM_VERTICAL,
                MainEncoderGlobalChord.resolve(1, false, false, true, true));
    }

    @Test
    void ignoresZeroIncrementsAndPopupBrowserTurns() {
        assertEquals(MainEncoderGlobalChord.Action.NONE,
                MainEncoderGlobalChord.resolve(0, false, false, true, false));
        assertEquals(MainEncoderGlobalChord.Action.NONE,
                MainEncoderGlobalChord.resolve(1, true, false, true, false));
    }
}
