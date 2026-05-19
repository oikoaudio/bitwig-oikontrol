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
                            final TouchResetControl touchReset,
                            final ResetPolicy resetPolicy,
                            final ValueDisplay display,
                            final Runnable clearDisplay) {
        bind(encoder, layer, encoderIndex, parameter, fallbackLabel, fineSupplier, touchReset, resetPolicy,
                ExplicitResetControl.none(), display, clearDisplay);
    }

    public static void bind(final TouchEncoder encoder,
                            final Layer layer,
                            final int encoderIndex,
                            final Parameter parameter,
                            final String fallbackLabel,
                            final BooleanSupplier fineSupplier,
                            final TouchResetControl touchReset,
                            final ResetPolicy resetPolicy,
                            final ExplicitResetControl explicitReset,
                            final ValueDisplay display,
                            final Runnable clearDisplay) {
        markInterested(parameter);
        encoder.bindContinuousEncoder(layer, fineSupplier, ContinuousEncoderScaler.Profile.STRONG, inc -> {
            if (!isMapped(parameter)) {
                return;
            }
            touchReset.markAdjusted(encoderIndex, Math.abs(inc));
            adjustParameter(parameter, fineSupplier.getAsBoolean(), inc);
            showValue(parameter, fallbackLabel, display);
        });
        encoder.bindTouched(layer, touched -> {
            if (touched) {
                handleTouchStart(encoderIndex, parameter, fallbackLabel, touchReset, resetPolicy, explicitReset, display);
            } else {
                touchReset.end(encoderIndex);
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
        EncoderValueProfile.LARGE_RANGE.adjustParameter(parameter, fine, inc);
    }

    public static String labelFor(final Parameter parameter, final String fallbackLabel) {
        final String name = parameter.name().get();
        return name == null || name.isBlank() ? fallbackLabel : name;
    }

    private static void handleTouchStart(final int encoderIndex,
                                         final Parameter parameter,
                                         final String fallbackLabel,
                                         final TouchResetControl touchReset,
                                         final ResetPolicy resetPolicy,
                                         final ExplicitResetControl explicitReset,
                                         final ValueDisplay display) {
        if (handleExplicitResetTouch(true, explicitReset, isMapped(parameter) && resetPolicy != ResetPolicy.NONE,
                fallbackLabel, isMapped(parameter) ? "No reset here" : "Unmapped",
                () -> resetPolicy.reset(parameter),
                () -> showValue(parameter, fallbackLabel, display),
                display)) {
            return;
        }
        if (!isMapped(parameter)) {
            display.show(fallbackLabel, "Unmapped");
            return;
        }
        if (resetPolicy != ResetPolicy.NONE) {
            touchReset.begin(encoderIndex, () -> {
                resetPolicy.reset(parameter);
                showValue(parameter, fallbackLabel, display);
            });
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
        },
        ORIGIN {
            @Override
            public void reset(final Parameter parameter) {
                parameter.value().setImmediately(parameter.getOrigin().get());
            }
        };

        public abstract void reset(Parameter parameter);
    }

    public interface TouchResetControl {
        void begin(int encoderIndex, Runnable resetAction);

        void markAdjusted(int encoderIndex, int units);

        void end(int encoderIndex);

        static TouchResetControl of(final EncoderTouchResetHandler handler) {
            return new TouchResetControl() {
                @Override
                public void begin(final int encoderIndex, final Runnable resetAction) {
                    handler.beginTouchReset(encoderIndex, resetAction);
                }

                @Override
                public void markAdjusted(final int encoderIndex, final int units) {
                    handler.markAdjusted(encoderIndex, units);
                }

                @Override
                public void end(final int encoderIndex) {
                    handler.endTouchReset(encoderIndex);
                }
            };
        }
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
