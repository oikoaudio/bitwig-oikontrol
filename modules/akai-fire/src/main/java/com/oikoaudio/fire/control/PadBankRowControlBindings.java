package com.oikoaudio.fire.control;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PadBankRowControlBindings {
    private final AkaiFireOikontrolExtension driver;
    private final Layer layer;
    private final Host host;
    private final boolean velocitySensitivePads;
    private final ExtraButtonBinding[] extraButtonBindings;

    public PadBankRowControlBindings(final AkaiFireOikontrolExtension driver, final Layer layer, final Host host,
                                     final ExtraButtonBinding... extraButtonBindings) {
        this(driver, layer, host, false, extraButtonBindings);
    }

    public static PadBankRowControlBindings velocitySensitivePads(final AkaiFireOikontrolExtension driver,
                                                                  final Layer layer,
                                                                  final Host host,
                                                                  final ExtraButtonBinding... extraButtonBindings) {
        return new PadBankRowControlBindings(driver, layer, host, true, extraButtonBindings);
    }

    private PadBankRowControlBindings(final AkaiFireOikontrolExtension driver, final Layer layer, final Host host,
                                      final boolean velocitySensitivePads,
                                      final ExtraButtonBinding... extraButtonBindings) {
        this.driver = driver;
        this.layer = layer;
        this.host = host;
        this.velocitySensitivePads = velocitySensitivePads;
        this.extraButtonBindings = extraButtonBindings;
    }

    public void bind() {
        bindPads();
        bindButtons();
    }

    private void bindPads() {
        if (velocitySensitivePads) {
            PadMatrixBindings.bindPressedVelocity(layer, driver.getRgbButtons(), new PadMatrixBindings.Host() {
                @Override
                public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
                    host.handlePadPress(padIndex, pressed, velocity);
                }

                @Override
                public RgbLigthState padLight(final int padIndex) {
                    return host.padLight(padIndex);
                }
            });
            return;
        }
        PadMatrixBindings.bindPressed(layer, driver.getRgbButtons(), new PadMatrixBindings.PressHost() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed) {
                host.handlePadPress(padIndex, pressed);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return host.padLight(padIndex);
            }
        });
    }

    private void bindButtons() {
        for (final ExtraButtonBinding binding : extraButtonBindings) {
            driver.getButton(binding.assignment()).bindPressed(layer, binding.handler(), binding.lightState());
        }
        BankButtonBindings.bind(layer, driver.getButton(NoteAssign.BANK_L), driver.getButton(NoteAssign.BANK_R),
                new BankButtonBindings.Host() {
                    @Override
                    public void handleBankButton(final boolean pressed, final int amount) {
                        host.handleBankButton(pressed, amount);
                    }

                    @Override
                    public BiColorLightState bankLightState() {
                        return host.bankLightState();
                    }

                    @Override
                    public BiColorLightState bankLightState(final int amount) {
                        return host.bankLightState(amount);
                    }
                });
        final BiColorButton[] rowButtons = {
                driver.getButton(NoteAssign.MUTE_1),
                driver.getButton(NoteAssign.MUTE_2),
                driver.getButton(NoteAssign.MUTE_3),
                driver.getButton(NoteAssign.MUTE_4)
        };
        ButtonRowBindings.bindPressed(layer, rowButtons, new ButtonRowBindings.Host() {
            @Override
            public void handleButton(final int index, final boolean pressed) {
                host.handleRowButton(index, pressed);
            }

            @Override
            public BiColorLightState lightState(final int index) {
                return host.rowLightState(index);
            }
        });
    }

    public interface Host {
        void handlePadPress(int padIndex, boolean pressed);

        default void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
            handlePadPress(padIndex, pressed);
        }

        RgbLigthState padLight(int padIndex);

        void handleBankButton(boolean pressed, int amount);

        BiColorLightState bankLightState();

        default BiColorLightState bankLightState(final int amount) {
            return bankLightState();
        }

        void handleRowButton(int index, boolean pressed);

        BiColorLightState rowLightState(int index);
    }

    public record ExtraButtonBinding(NoteAssign assignment, Consumer<Boolean> handler,
                                     Supplier<BiColorLightState> lightState) {
    }
}
