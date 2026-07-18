package com.oikoaudio.fire.multiclip;

/** Immutable navigation state for four visible Track Lanes and sixteen visible steps. */
public record MulticlipPageState(
        int laneCount, int lanePage, int activeChildPosition, int firstVisibleStep) {
    public static final int STEPS_PER_PAGE = 16;

    public static MulticlipPageState initial(final int laneCount) {
        if (laneCount < 0 || laneCount > TrackLaneMapping.MAX_LANES) {
            throw new IllegalArgumentException("Lane count must be between 0 and 16");
        }
        return new MulticlipPageState(laneCount, 0, laneCount == 0 ? -1 : 0, 0);
    }

    public int activeRow() {
        return activeChildPosition < 0
                ? -1
                : activeChildPosition % TrackLaneMapping.LANES_PER_PAGE;
    }

    public MulticlipPageState withActiveChildPosition(final int childPosition) {
        if (childPosition < 0 || childPosition >= laneCount) {
            throw new IllegalArgumentException("Active child position is outside the lane bank");
        }
        return new MulticlipPageState(
                laneCount,
                childPosition / TrackLaneMapping.LANES_PER_PAGE,
                childPosition,
                firstVisibleStep);
    }

    public MulticlipPageState pageLanes(final int direction) {
        if (laneCount == 0 || direction == 0) {
            return this;
        }

        final int lastPage = (laneCount - 1) / TrackLaneMapping.LANES_PER_PAGE;
        final int targetPage = Math.max(0, Math.min(lastPage, lanePage + Integer.signum(direction)));
        final int targetPosition = Math.min(
                targetPage * TrackLaneMapping.LANES_PER_PAGE + Math.max(0, activeRow()),
                laneCount - 1);
        return new MulticlipPageState(laneCount, targetPage, targetPosition, firstVisibleStep);
    }

    public boolean canPageLanes(final int direction) {
        if (laneCount == 0 || direction == 0) {
            return false;
        }
        final int lastPage = (laneCount - 1) / TrackLaneMapping.LANES_PER_PAGE;
        return direction < 0 ? lanePage > 0 : lanePage < lastPage;
    }

    public MulticlipPageState pageTime(final int direction) {
        if (direction == 0) {
            return this;
        }
        final int firstStep = Math.max(0, firstVisibleStep + Integer.signum(direction) * STEPS_PER_PAGE);
        return new MulticlipPageState(laneCount, lanePage, activeChildPosition, firstStep);
    }

    public boolean canPageTime(final int direction) {
        return direction > 0 || (direction < 0 && firstVisibleStep > 0);
    }
}
