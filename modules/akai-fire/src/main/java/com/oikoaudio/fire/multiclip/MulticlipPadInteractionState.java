package com.oikoaudio.fire.multiclip;

import java.util.Arrays;

/** Pure held-pad session state, including delayed toggle and nudge consumption. */
final class MulticlipPadInteractionState {
    private static final int PAD_COUNT = 64;
    private static final int STEPS_PER_ROW = 16;

    private final boolean[] held = new boolean[PAD_COUNT];
    private final boolean[] occupiedAtPress = new boolean[PAD_COUNT];
    private final boolean[] consumed = new boolean[PAD_COUNT];

    void press(final int padIndex) {
        held[padIndex] = true;
        occupiedAtPress[padIndex] = false;
        consumed[padIndex] = false;
    }

    void captureOccupied(final int padIndex, final boolean occupied) {
        occupiedAtPress[padIndex] = occupied;
    }

    void release(final int padIndex) {
        held[padIndex] = false;
        occupiedAtPress[padIndex] = false;
        consumed[padIndex] = false;
    }

    boolean isHeld(final int padIndex) {
        return held[padIndex];
    }

    boolean wasOccupied(final int padIndex) {
        return occupiedAtPress[padIndex];
    }

    boolean isConsumed(final int padIndex) {
        return consumed[padIndex];
    }

    boolean hasHeldPads() {
        for (final boolean value : held) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    void consumeHeldRow(final int row) {
        final int start = row * STEPS_PER_ROW;
        for (int step = 0; step < STEPS_PER_ROW; step++) {
            if (held[start + step]) {
                consumed[start + step] = true;
            }
        }
    }

    void clear() {
        Arrays.fill(held, false);
        Arrays.fill(occupiedAtPress, false);
        Arrays.fill(consumed, false);
    }
}
