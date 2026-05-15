package com.oikoaudio.fire.control;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.lights.RgbLigthState;

public final class PadMatrixBindings {
    private PadMatrixBindings() {
    }

    public static void bindPressedVelocity(final Layer layer, final RgbButton[] pads, final Host host) {
        for (int index = 0; index < pads.length; index++) {
            final int padIndex = index;
            pads[index].bindPressedVelocity(layer, velocity -> host.handlePadPress(padIndex, true, velocity),
                    () -> host.handlePadPress(padIndex, false, 0), () -> host.padLight(padIndex));
        }
    }

    public interface Host {
        void handlePadPress(int padIndex, boolean pressed, int velocity);

        RgbLigthState padLight(int padIndex);
    }
}
