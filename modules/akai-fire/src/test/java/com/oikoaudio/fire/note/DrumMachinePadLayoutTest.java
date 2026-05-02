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

    @Test
    void velocityUsesLeftBlockAsSelectorsAndRemainderAsSelectedSound() {
        final DrumMachinePadLayout layout = new DrumMachinePadLayout(36,
                DrumMachinePadLayout.Layout.VELOCITY, 7);

        assertEquals(0, layout.selectorOffsetForPad(48));
        assertEquals(7, layout.selectorOffsetForPad(35));
        assertEquals(36, layout.noteForPad(48));
        assertEquals(43, layout.noteForPad(52));
        assertEquals(7, layout.padBankIndexForPad(52));
    }

    @Test
    void bongosSplitsPlayableAreaAcrossSelectedSounds() {
        final DrumMachinePadLayout layout = new DrumMachinePadLayout(36,
                DrumMachinePadLayout.Layout.BONGOS, 7, 12);

        assertEquals(-1, layout.noteForPad(52));
        assertEquals(-1, layout.noteForPad(58));
        assertEquals(43, layout.noteForPad(53));
        assertEquals(48, layout.noteForPad(59));
        assertEquals(7, layout.padBankIndexForPad(53));
        assertEquals(12, layout.padBankIndexForPad(59));
    }
}
