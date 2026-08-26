package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DrumPadDeviceSelectionTest {
    @Test
    void movesBetweenExistingDevicesAndClampsAtEnds() {
        final boolean[] existing = {true, false, true, true, false};

        assertEquals(2, DrumPadDeviceSelection.nextIndex(0, 1, existing));
        assertEquals(3, DrumPadDeviceSelection.nextIndex(2, 1, existing));
        assertEquals(3, DrumPadDeviceSelection.nextIndex(3, 1, existing));
        assertEquals(2, DrumPadDeviceSelection.nextIndex(3, -1, existing));
        assertEquals(0, DrumPadDeviceSelection.nextIndex(0, -1, existing));
    }

    @Test
    void reportsNoSelectionForAnEmptyChain() {
        assertEquals(-1, DrumPadDeviceSelection.nextIndex(-1, 1, new boolean[4]));
    }
}
