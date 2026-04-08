package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

public final class MelodicRenderer {
    public static final RgbLigthState ACTIVE_STEP = new RgbLigthState(0, 88, 32, true);
    public static final RgbLigthState SELECTED_STEP = new RgbLigthState(112, 96, 0, true);
    public static final RgbLigthState TIED_STEP = new RgbLigthState(16, 70, 70, true);
    public static final RgbLigthState ACCENT_STEP = new RgbLigthState(0, 110, 60, true);
    public static final RgbLigthState OUT_OF_LOOP = new RgbLigthState(12, 12, 18, true);
    public static final RgbLigthState PROCESS = new RgbLigthState(32, 48, 96, true);
    public static final RgbLigthState PROCESS_SELECTED = new RgbLigthState(96, 72, 24, true);
    public static final RgbLigthState PITCH_POOL_ON = new RgbLigthState(0, 76, 112, true);
    public static final RgbLigthState PITCH_POOL_OFF = RgbLigthState.GRAY_1;
    public static final RgbLigthState PITCH_POOL_ROOT = new RgbLigthState(88, 80, 0, true);
    public static final RgbLigthState PITCH_POOL_USED = new RgbLigthState(0, 116, 68, true);

    private MelodicRenderer() {
    }

    public static RgbLigthState stepLight(final MelodicPattern.Step step, final boolean selected,
                                          final boolean inLoop, final boolean playing, final int stepIndex) {
        if (!inLoop) {
            return OUT_OF_LOOP;
        }
        if (selected) {
            return playing ? SELECTED_STEP.getBrightest() : SELECTED_STEP;
        }
        if (step.tieFromPrevious()) {
            return playing ? TIED_STEP.getBrightest() : TIED_STEP;
        }
        if (!step.active()) {
            return StepPadLightHelper.renderEmptyStep(stepIndex, playing ? stepIndex : -1);
        }
        final RgbLigthState base = step.accent() ? ACCENT_STEP : ACTIVE_STEP;
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
