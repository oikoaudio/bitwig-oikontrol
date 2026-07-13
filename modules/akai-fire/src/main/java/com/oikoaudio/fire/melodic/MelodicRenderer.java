package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

public final class MelodicRenderer {
    public static final RgbLightState ACTIVE_STEP = new RgbLightState(0, 88, 32, true);
    public static final RgbLightState HELD_STEP = RgbLightState.PURPLE;
    public static final RgbLightState OUT_OF_LOOP = new RgbLightState(12, 12, 18, true);
    public static final RgbLightState PITCH_POOL_ON = new RgbLightState(0, 76, 112, true);
    public static final RgbLightState PITCH_POOL_OFF = RgbLightState.GRAY_1;
    public static final RgbLightState PITCH_POOL_ROOT = new RgbLightState(88, 80, 0, true);
    public static final RgbLightState PITCH_POOL_USED = RgbLightState.PURPLE;

    private MelodicRenderer() {}

    public static RgbLightState stepLight(
            final MelodicPattern.Step step,
            final boolean held,
            final boolean inLoop,
            final boolean playing,
            final int stepIndex,
            final RgbLightState clipColor) {
        if (!inLoop) {
            return OUT_OF_LOOP;
        }
        if (held) {
            return playing ? HELD_STEP.getBrightest() : HELD_STEP;
        }
        final RgbLightState baseColor = clipColor != null ? clipColor : ACTIVE_STEP;
        if (step.tieFromPrevious()) {
            final RgbLightState tied = baseColor.getDimmed();
            return playing ? StepPadLightHelper.renderPlayheadHighlight(tied) : tied;
        }
        if (!step.active()) {
            return StepPadLightHelper.renderEmptyStep(stepIndex, playing ? stepIndex : -1);
        }
        if (playing) {
            return StepPadLightHelper.renderPlayheadHighlight(
                    step.accent() ? baseColor.getBrightend() : baseColor);
        }
        return StepPadLightHelper.renderOccupiedStep(baseColor, step.accent(), false);
    }

    public static RgbLightState pitchPoolLight(
            final boolean enabled, final boolean root, final boolean usedInPattern) {
        if (enabled) {
            final RgbLightState base =
                    usedInPattern ? PITCH_POOL_USED : (root ? PITCH_POOL_ROOT : PITCH_POOL_ON);
            return base;
        }
        return root ? PITCH_POOL_ROOT.getDimmed() : PITCH_POOL_OFF;
    }
}
