package com.oikoaudio.fire.control;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.lights.BiColorLightState;

public final class BankButtonBindings {
    private BankButtonBindings() {
    }

    public static void bind(final Layer layer, final BiColorButton leftButton, final BiColorButton rightButton,
                            final Host host) {
        leftButton.bindPressed(layer, pressed -> host.handleBankButton(pressed, -1),
                () -> host.bankLightState(-1));
        rightButton.bindPressed(layer, pressed -> host.handleBankButton(pressed, 1),
                () -> host.bankLightState(1));
    }

    public interface Host {
        void handleBankButton(boolean pressed, int amount);

        BiColorLightState bankLightState();

        default BiColorLightState bankLightState(final int amount) {
            return bankLightState();
        }
    }
}
