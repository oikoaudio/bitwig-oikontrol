package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DrumMachinePadLayoutTest {

    @Test
    void mapsBottomRowFromFirstVisibleDrumPad() {
        final DrumMachinePadLayout layout = new DrumMachinePadLayout(36);

        assertEquals(36, layout.noteForPad(48));
        assertEquals(37, layout.noteForPad(49));
        assertEquals(51, layout.noteForPad(63));
        assertEquals(84, layout.noteForPad(0));
        assertArrayEquals(new int[]{36}, layout.notesForPad(48));
    }

    @Test
    void exposesPadBankIndexForObservedNamesAndColors() {
        final DrumMachinePadLayout layout = new DrumMachinePadLayout(0);

        assertEquals(0, layout.padBankIndexForPad(48));
        assertEquals(15, layout.padBankIndexForPad(63));
        assertEquals(48, layout.padBankIndexForPad(0));
        assertEquals(63, layout.padBankIndexForPad(15));
    }

    @Test
    void followsScrolledDrumPadWindow() {
        final DrumMachinePadLayout layout = new DrumMachinePadLayout(48);

        assertEquals(48, layout.noteForPad(48));
        assertEquals(96, layout.noteForPad(0));
    }
}
