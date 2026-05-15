package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.RgbLigthState;

/**
 * Shared pad-light rendering for step rows.
 * Encapsulates the visual policy for empty steps, occupied steps, accent emphasis, and playhead
 * highlighting so the modes do not duplicate RGB decisions.
 */
public final class StepPadLightHelper {
    private static final int PLAYHEAD_WHITE_FLOOR = 96;
    private static final double POSITION_EPSILON = 0.0001;
    private static final RgbLigthState CLIP_START_INDICATOR = new RgbLigthState(30, 0, 127, true);

    private StepPadLightHelper() {
    }

    public static RgbLigthState renderEmptyStep(final int stepIndex, final int playingStep) {
        if (stepIndex == playingStep) {
            return RgbLigthState.WHITE;
        }
        return (stepIndex / 4) % 2 == 0 ? RgbLigthState.GRAY_1 : RgbLigthState.GRAY_2;
    }

    public static boolean isStepWithinVisibleLoop(final int stepIndex, final int visibleLoopSteps) {
        return stepIndex >= 0 && stepIndex < visibleLoopSteps;
    }

    public static RgbLigthState renderOccupiedStep(final RgbLigthState base, final boolean accented,
                                                   final boolean playing) {
        if (accented) {
            return base.getBrightend();
        }
        if (playing) {
            return base.getBrightend();
        }
        return base;
    }

    public static RgbLigthState renderPlayheadHighlight(final RgbLigthState base) {
        final RgbLigthState brightest = base.getBrightest();
        return new RgbLigthState(
                Math.max(brightest.getRed() & 0xFF, PLAYHEAD_WHITE_FLOOR),
                Math.max(brightest.getGreen() & 0xFF, PLAYHEAD_WHITE_FLOOR),
                Math.max(brightest.getBlue() & 0xFF, PLAYHEAD_WHITE_FLOOR),
                true);
    }

    public static RgbLigthState renderClipStartIndicator(final RgbLigthState base) {
        return CLIP_START_INDICATOR;
    }

    public static RgbLigthState renderClipStartColumnOverlay(final int padColumn,
                                                             final int shiftedClipStartColumn,
                                                             final RgbLigthState base) {
        return shiftedClipStartColumn >= 0 && shiftedClipStartColumn == padColumn
                ? renderClipStartIndicator(base)
                : base;
    }

    public static int nearestVisibleStepForShiftedClipStart(final double playStart,
                                                           final double loopLength,
                                                           final double stepLength,
                                                           final int visibleStepOffset,
                                                           final int visibleStepCount) {
        if (stepLength <= 0.0 || visibleStepCount <= 0) {
            return -1;
        }
        final double wrapped = shiftedPlayStart(playStart, loopLength);
        if (wrapped < 0.0) {
            return -1;
        }
        final int totalSteps = Math.max(1, (int) Math.round(Math.max(stepLength, loopLength) / stepLength));
        final int globalStep = Math.floorMod((int) Math.round(wrapped / stepLength), totalSteps);
        final int localStep = globalStep - visibleStepOffset;
        return localStep >= 0 && localStep < visibleStepCount ? localStep : -1;
    }

    public static int nearestVisibleColumnForShiftedClipStart(final double playStart,
                                                              final double loopLength,
                                                              final double stepLength,
                                                              final int visibleStepOffset,
                                                              final int visibleStepCount,
                                                              final int columnCount) {
        final int localStep = nearestVisibleStepForShiftedClipStart(
                playStart, loopLength, stepLength, visibleStepOffset, visibleStepCount);
        return localStep < 0 || columnCount <= 0 ? -1 : Math.floorMod(localStep, columnCount);
    }

    public static int nearestBucketForShiftedClipStart(final double playStart,
                                                       final double loopLength,
                                                       final int bucketCount) {
        if (bucketCount <= 0) {
            return -1;
        }
        final double wrapped = shiftedPlayStart(playStart, loopLength);
        if (wrapped < 0.0) {
            return -1;
        }
        final double normalized = wrapped / Math.max(POSITION_EPSILON, loopLength);
        return Math.floorMod((int) Math.round(normalized * bucketCount), bucketCount);
    }

    public static int nearestColumnForShiftedClipStart(final double playStart,
                                                       final double loopLength,
                                                       final int columnCount) {
        final int bucket = nearestBucketForShiftedClipStart(playStart, loopLength, columnCount);
        return bucket < 0 ? -1 : Math.floorMod(bucket, columnCount);
    }

    private static double shiftedPlayStart(final double playStart, final double loopLength) {
        final double length = Math.max(POSITION_EPSILON, loopLength);
        final double wrapped = playStart % length;
        final double normalized = wrapped < 0.0 ? wrapped + length : wrapped;
        return normalized > POSITION_EPSILON && length - normalized > POSITION_EPSILON ? normalized : -1.0;
    }
}
