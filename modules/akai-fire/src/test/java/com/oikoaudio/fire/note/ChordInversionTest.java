package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ChordInversionTest {

    @Test
    void rotateUpMovesLowestNoteUpAnOctave() {
        assertArrayEquals(new int[]{64, 67, 72}, ChordInversion.rotate(new int[]{60, 64, 67}, 1));
    }

    @Test
    void rotateDownMovesHighestNoteDownAnOctave() {
        assertArrayEquals(new int[]{55, 60, 64}, ChordInversion.rotate(new int[]{60, 64, 67}, -1));
    }

    @Test
    void rotateSortsInputBeforeApplyingInversion() {
        assertArrayEquals(new int[]{64, 67, 72}, ChordInversion.rotate(new int[]{67, 60, 64}, 1));
    }

    @Test
    void rotateLeavesSingleNoteUnchanged() {
        assertArrayEquals(new int[]{60}, ChordInversion.rotate(new int[]{60}, 1));
    }

    @Test
    void rotateLeavesEmptyChordUnchanged() {
        assertArrayEquals(new int[0], ChordInversion.rotate(new int[0], 1));
    }
}
