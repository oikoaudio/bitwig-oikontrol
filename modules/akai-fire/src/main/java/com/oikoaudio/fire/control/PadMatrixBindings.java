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

    public static void bindPressed(final Layer layer, final RgbButton[] pads, final PressHost host) {
        bindPressed(layer, pads, 0, pads.length, host);
    }

    public static void bindPressed(final Layer layer, final RgbButton[] pads, final int startIndex,
                                   final int count, final PressHost host) {
        for (int index = 0; index < pads.length; index++) {
            if (index < startIndex || index >= startIndex + count) {
                continue;
            }
            final int padIndex = index - startIndex;
            pads[index].bindPressed(layer, pressed -> host.handlePadPress(padIndex, pressed),
                    () -> host.padLight(padIndex));
        }
    }

    public interface Host {
        void handlePadPress(int padIndex, boolean pressed, int velocity);

        RgbLigthState padLight(int padIndex);
    }

    public interface PressHost {
        void handlePadPress(int padIndex, boolean pressed);

        RgbLigthState padLight(int padIndex);
    }
}
