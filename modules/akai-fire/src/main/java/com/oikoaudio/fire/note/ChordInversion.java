package com.oikoaudio.fire.note;

import java.util.Arrays;

/**
 * Pure chord inversion helper for chord-step editing.
 * Rotates the lowest note up an octave or the highest note down an octave while preserving sorted order.
 */
public final class ChordInversion {
    private ChordInversion() {
    }

    public static int[] rotate(final int[] notes, final int direction) {
        final int[] sorted = Arrays.stream(notes).sorted().toArray();
        if (sorted.length <= 1) {
            return sorted;
        }
        final int[] rotated = Arrays.copyOf(sorted, sorted.length);
        if (direction >= 0) {
            final int first = rotated[0];
            System.arraycopy(rotated, 1, rotated, 0, rotated.length - 1);
            rotated[rotated.length - 1] = first + 12;
        } else {
            final int last = rotated[rotated.length - 1];
            System.arraycopy(rotated, 0, rotated, 1, rotated.length - 1);
            rotated[0] = last - 12;
        }
        Arrays.sort(rotated);
        return rotated;
    }
}
