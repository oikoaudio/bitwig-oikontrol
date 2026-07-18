package com.oikoaudio.fire.multiclip;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Controller-owned observation state for the four visible lane clips. */
public final class MulticlipLaneState {
    private static final int ROWS = 4;
    private static final int STEPS = 16;

    private final boolean[][] occupied = new boolean[ROWS][STEPS];
    private final Set<Integer>[][] observedChannels;
    private final int[] playingSteps = {-1, -1, -1, -1};

    @SuppressWarnings("unchecked")
    public MulticlipLaneState() {
        observedChannels = new Set[ROWS][STEPS];
        for (int row = 0; row < ROWS; row++) {
            for (int step = 0; step < STEPS; step++) {
                observedChannels[row][step] = new HashSet<>();
            }
        }
    }

    public boolean isOccupied(final int row, final int step) {
        return occupied[row][step];
    }

    public void setOccupied(final int row, final int step, final boolean value) {
        occupied[row][step] = value;
        if (!value) {
            observedChannels[row][step].clear();
        }
    }

    public void observeChannel(
            final int row, final int step, final int channel, final boolean noteOn) {
        if (noteOn) {
            observedChannels[row][step].add(channel);
        } else {
            observedChannels[row][step].remove(channel);
        }
        occupied[row][step] = !observedChannels[row][step].isEmpty();
    }

    public Set<Integer> channelsAt(final int row, final int step) {
        return Set.copyOf(observedChannels[row][step]);
    }

    public boolean isPlaying(final int row, final int step) {
        return playingSteps[row] == step;
    }

    public void setPlayingStep(final int row, final int step) {
        playingSteps[row] = step;
    }

    public void clearRow(final int row) {
        Arrays.fill(occupied[row], false);
        for (int step = 0; step < STEPS; step++) {
            observedChannels[row][step].clear();
        }
        playingSteps[row] = -1;
    }
}
