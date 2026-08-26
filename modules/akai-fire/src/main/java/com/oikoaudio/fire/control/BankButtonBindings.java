package com.oikoaudio.fire.control;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.lights.BiColorLightState;

public final class BankButtonBindings {
    private BankButtonBindings() {}

    public static void bind(
            final Layer layer,
            final BiColorButton leftButton,
            final BiColorButton rightButton,
            final GestureInterceptor interceptor,
            final Host host) {
        leftButton.bindPressed(
                layer,
                pressed -> {
                    if (!interceptor.handle(pressed, -1)) {
                        host.handleBankButton(pressed, -1);
                    }
                },
                () -> host.bankLightState(-1));
        rightButton.bindPressed(
                layer,
                pressed -> {
                    if (!interceptor.handle(pressed, 1)) {
                        host.handleBankButton(pressed, 1);
                    }
                },
                () -> host.bankLightState(1));
    }

    @FunctionalInterface
    public interface GestureInterceptor {
        boolean handle(boolean pressed, int amount);
    }

    public interface Host {
        void handleBankButton(boolean pressed, int amount);

        BiColorLightState bankLightState();

        default BiColorLightState bankLightState(final int amount) {
            return bankLightState();
        }
    }
}
