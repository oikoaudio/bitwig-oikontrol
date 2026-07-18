package com.oikoaudio.fire.multiclip;

import java.util.Arrays;

/** Controller-owned observation state for the four visible lane clips. */
public final class MulticlipLaneState {
    private static final int ROWS = 4;
    private static final int STEPS = 16;

    private final boolean[][] occupied = new boolean[ROWS][STEPS];
    private final int[] playingSteps = {-1, -1, -1, -1};

    public boolean isOccupied(final int row, final int step) {
        return occupied[row][step];
    }

    public void setOccupied(final int row, final int step, final boolean value) {
        occupied[row][step] = value;
    }

    public boolean isPlaying(final int row, final int step) {
        return playingSteps[row] == step;
    }

    public void setPlayingStep(final int row, final int step) {
        playingSteps[row] = step;
    }

    public void clearRow(final int row) {
        Arrays.fill(occupied[row], false);
        playingSteps[row] = -1;
    }
}
