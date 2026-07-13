package com.oikoaudio.fire.fugue;

import com.oikoaudio.fire.control.TrackSelectIndicatorLights;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.melodic.MelodicPattern;
import com.oikoaudio.fire.melodic.MelodicRenderer;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

/** Pure pad and line-light policy for Fugue mode. */
public final class FugueRenderer {
    private static final int COLUMNS = 16;

    private FugueRenderer() {
    }

    public static RgbLightState padLight(final FuguePattern pattern, final int column, final int loopSteps,
                                         final int playingStep, final int shiftedStartColumn,
                                         final RgbLightState lineColor, final boolean selected,
                                         final boolean enabled) {
        final boolean inLoop = column < activeBucketCount(loopSteps);
        final boolean playing = playingBucket(playingStep, loopSteps) == column;
        final RgbLightState color = selected ? lineColor.getBrightend() : lineColor;
        final RgbLightState rendered = MelodicRenderer.stepLight(
                bucketStep(pattern, column, loopSteps), false, inLoop, playing, column, color);
        final RgbLightState enabledState = enabled ? rendered : rendered.getVeryDimmed();
        return inLoop
                ? StepPadLightHelper.renderClipStartColumnOverlay(column, shiftedStartColumn, enabledState)
                : enabledState;
    }

    public static MelodicPattern.Step bucketStep(final FuguePattern pattern, final int column, final int loopSteps) {
        if (column >= activeBucketCount(loopSteps)) {
            return MelodicPattern.Step.rest(column);
        }
        for (int step = bucketStart(column, loopSteps); step < bucketEnd(column, loopSteps); step++) {
            final MelodicPattern.Step candidate = pattern.step(step);
            if (candidate.active() && !candidate.tieFromPrevious()) {
                return candidate.withIndex(column);
            }
        }
        return MelodicPattern.Step.rest(column);
    }

    public static int activeBucketCount(final int loopSteps) {
        return Math.max(1, Math.min(COLUMNS, loopSteps));
    }

    public static int bucketStart(final int column, final int loopSteps) {
        return column * loopSteps / COLUMNS;
    }

    public static int bucketEnd(final int column, final int loopSteps) {
        return Math.max(bucketStart(column, loopSteps) + 1, (column + 1) * loopSteps / COLUMNS);
    }

    public static int playingBucket(final int playingStep, final int loopSteps) {
        if (playingStep < 0 || playingStep >= loopSteps) {
            return -1;
        }
        return Math.max(0, Math.min(COLUMNS - 1, playingStep * COLUMNS / loopSteps));
    }

    public static BiColorLightState lineLight(final boolean enabled) {
        return enabled ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    public static BiColorLightState lineStatusLight(final boolean enabled) {
        return enabled ? TrackSelectIndicatorLights.green(true) : TrackSelectIndicatorLights.red(false);
    }
}
