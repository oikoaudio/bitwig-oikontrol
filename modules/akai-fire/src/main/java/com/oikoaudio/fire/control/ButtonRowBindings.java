package com.oikoaudio.fire.control;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.lights.BiColorLightState;

public final class ButtonRowBindings {
    private ButtonRowBindings() {
    }

    public static void bindPressed(final Layer layer, final BiColorButton[] buttons, final Host host) {
        for (int index = 0; index < buttons.length; index++) {
            final int buttonIndex = index;
            buttons[index].bindPressed(layer, pressed -> host.handleButton(buttonIndex, pressed),
                    () -> host.lightState(buttonIndex));
        }
    }

    public interface Host {
        void handleButton(int index, boolean pressed);

        BiColorLightState lightState(int index);
    }
}
