package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

public final class MelodicRenderer {
    public static final RgbLigthState ACTIVE_STEP = new RgbLigthState(0, 88, 32, true);
    public static final RgbLigthState HELD_STEP = RgbLigthState.PURPLE;
    public static final RgbLigthState OUT_OF_LOOP = new RgbLigthState(12, 12, 18, true);
    public static final RgbLigthState PITCH_POOL_ON = new RgbLigthState(0, 76, 112, true);
    public static final RgbLigthState PITCH_POOL_OFF = RgbLigthState.GRAY_1;
    public static final RgbLigthState PITCH_POOL_ROOT = new RgbLigthState(88, 80, 0, true);
    public static final RgbLigthState PITCH_POOL_USED = RgbLigthState.PURPLE;

    private MelodicRenderer() {
    }

    public static RgbLigthState stepLight(final MelodicPattern.Step step, final boolean held,
                                          final boolean inLoop, final boolean playing, final int stepIndex,
                                          final RgbLigthState clipColor) {
        if (!inLoop) {
            return OUT_OF_LOOP;
        }
        if (held) {
            return playing ? HELD_STEP.getBrightest() : HELD_STEP;
        }
        final RgbLigthState baseColor = clipColor != null ? clipColor : ACTIVE_STEP;
        if (step.tieFromPrevious()) {
            final RgbLigthState tied = baseColor.getSoftDimmed();
            return playing ? tied.getBrightend() : tied;
        }
        if (!step.active()) {
            return StepPadLightHelper.renderEmptyStep(stepIndex, playing ? stepIndex : -1);
        }
        final RgbLigthState base = step.accent() ? baseColor.getBrightend() : baseColor.getSoftDimmed();
        return playing ? base.getBrightest() : base;
    }

    public static RgbLigthState pitchPoolLight(final boolean enabled, final boolean root, final boolean usedInPattern) {
        if (enabled) {
            final RgbLigthState base = usedInPattern ? PITCH_POOL_USED : (root ? PITCH_POOL_ROOT : PITCH_POOL_ON);
            return base;
        }
        return root ? PITCH_POOL_ROOT.getDimmed() : PITCH_POOL_OFF;
    }
}
