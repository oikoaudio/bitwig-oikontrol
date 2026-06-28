package com.oikoaudio.fire.control;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Layer;

import java.util.function.BooleanSupplier;

public final class ParameterEncoderBinding {
    private ParameterEncoderBinding() {
    }

    public static void markInterested(final Parameter parameter) {
        parameter.exists().markInterested();
        parameter.name().markInterested();
        parameter.displayedValue().markInterested();
        parameter.value().markInterested();
        parameter.discreteValueCount().markInterested();
        parameter.getOrigin().markInterested();
    }

    public static void bind(final TouchEncoder encoder,
                            final Layer layer,
                            final int encoderIndex,
                            final Parameter parameter,
                            final String fallbackLabel,
                            final BooleanSupplier fineSupplier,
                            final ResetPolicy resetPolicy,
                            final ValueDisplay display,
                            final Runnable clearDisplay) {
        bind(encoder, layer, encoderIndex, parameter, fallbackLabel, fineSupplier, resetPolicy,
                ExplicitResetControl.none(), display, clearDisplay);
    }

    public static void bind(final TouchEncoder encoder,
                            final Layer layer,
                            final int encoderIndex,
                            final Parameter parameter,
                            final String fallbackLabel,
                            final BooleanSupplier fineSupplier,
                            final ResetPolicy resetPolicy,
                            final ExplicitResetControl explicitReset,
                            final ValueDisplay display,
                            final Runnable clearDisplay) {
        bind(encoder, layer, encoderIndex, parameter, fallbackLabel, fineSupplier, resetPolicy, explicitReset,
                EncoderValueProfile.LARGE_RANGE, display, clearDisplay);
    }

    public static void bind(final TouchEncoder encoder,
                            final Layer layer,
                            final int encoderIndex,
                            final Parameter parameter,
                            final String fallbackLabel,
                            final BooleanSupplier fineSupplier,
                            final ResetPolicy resetPolicy,
                            final ExplicitResetControl explicitReset,
                            final EncoderValueProfile profile,
                            final ValueDisplay display,
                            final Runnable clearDisplay) {
        markInterested(parameter);
        encoder.bindContinuousEncoder(layer, fineSupplier, ContinuousEncoderScaler.Profile.STRONG, inc -> {
            if (!isMapped(parameter)) {
                return;
            }
            adjustParameter(parameter, fineSupplier.getAsBoolean(), inc, profile);
            showValue(parameter, fallbackLabel, display);
        });
        encoder.bindTouched(layer, touched -> {
            if (touched) {
                handleTouchStart(parameter, fallbackLabel, resetPolicy, explicitReset, display);
            } else {
                clearDisplay.run();
            }
        });
    }

    public static void showValue(final Parameter parameter, final String fallbackLabel, final ValueDisplay display) {
        display.show(labelFor(parameter, fallbackLabel), parameter.displayedValue().get());
    }

    public static boolean isMapped(final Parameter parameter) {
        return parameter != null && parameter.exists().get();
    }

    public static void adjustParameter(final Parameter parameter, final boolean fine, final int inc) {
        adjustParameter(parameter, fine, inc, EncoderValueProfile.LARGE_RANGE);
    }

    public static void adjustParameter(final Parameter parameter, final boolean fine, final int inc,
                                       final EncoderValueProfile profile) {
        profile.adjustParameter(parameter, fine, inc);
    }

    public static String labelFor(final Parameter parameter, final String fallbackLabel) {
        final String name = parameter.name().get();
        return name == null || name.isBlank() ? fallbackLabel : name;
    }

    private static void handleTouchStart(final Parameter parameter,
                                         final String fallbackLabel,
                                         final ResetPolicy resetPolicy,
                                         final ExplicitResetControl explicitReset,
                                         final ValueDisplay display) {
        if (handleExplicitResetTouch(true, explicitReset, isMapped(parameter) && resetPolicy != ResetPolicy.NONE,
                fallbackLabel, isMapped(parameter) ? "No reset" : "Unmapped",
                () -> resetPolicy.reset(parameter),
                () -> showValue(parameter, fallbackLabel, display),
                display)) {
            return;
        }
        if (!isMapped(parameter)) {
            display.show(fallbackLabel, "Unmapped");
            return;
        }
        showValue(parameter, fallbackLabel, display);
    }

    public static boolean handleExplicitResetTouch(final boolean touched,
                                                   final ExplicitResetControl explicitReset,
                                                   final boolean resettable,
                                                   final String fallbackLabel,
                                                   final String unavailableDetail,
                                                   final Runnable resetAction,
                                                   final Runnable showAction,
                                                   final ValueDisplay display) {
        if (!touched || !explicitReset.isHeld()) {
            return false;
        }
        explicitReset.consume();
        if (!resettable) {
            display.show(fallbackLabel, unavailableDetail);
            return true;
        }
        resetAction.run();
        showAction.run();
        return true;
    }

    public enum ResetPolicy {
        NONE {
            @Override
            public void reset(final Parameter parameter) {
            }
        },
        PARAMETER_DEFAULT {
            @Override
            public void reset(final Parameter parameter) {
                parameter.reset();
            }
        };

        public abstract void reset(Parameter parameter);
    }

    public interface ExplicitResetControl {
        boolean isHeld();

        void consume();

        static ExplicitResetControl none() {
            return new ExplicitResetControl() {
                @Override
                public boolean isHeld() {
                    return false;
                }

                @Override
                public void consume() {
                }
            };
        }
    }

    @FunctionalInterface
    public interface ValueDisplay {
        void show(String title, String value);
    }
}
