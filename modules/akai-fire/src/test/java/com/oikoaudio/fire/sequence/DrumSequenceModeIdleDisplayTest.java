package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrumSequenceModeIdleDisplayTest {
    @Test
    void drumMetersOnlyShowWhileTransportIsPlaying() {
        assertEquals(true, DrumSequenceMode.shouldShowDrumMeters(
                true, false, false, false, false, false, false, true));
        assertEquals(false, DrumSequenceMode.shouldShowDrumMeters(
                true, false, false, false, false, false, false, false));
        assertEquals(false, DrumSequenceMode.shouldShowDrumMeters(
                true, true, false, false, false, false, false, true));
    }
}
