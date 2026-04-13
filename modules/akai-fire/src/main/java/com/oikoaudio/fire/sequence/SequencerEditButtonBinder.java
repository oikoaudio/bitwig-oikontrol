package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.lights.BiColorLightState;

/**
 * Shared binder for sequencer edit buttons that expose a held primary function and, optionally, a
 * shift-selected alternate function.
 */
public final class SequencerEditButtonBinder {
    private final Layer layer;
    private final BooleanValueObject shiftActive;
    private final EditFunctionFeedback feedback;

    public SequencerEditButtonBinder(final Layer layer,
                                     final BooleanValueObject shiftActive,
                                     final EditFunctionFeedback feedback) {
        this.layer = layer;
        this.shiftActive = shiftActive;
        this.feedback = feedback;
    }

    public void bind(final BiColorButton button,
                     final BooleanValueObject mainValue,
                     final MultiStateHardwareLight stateLight,
                     final BooleanValueObject altValue,
                     final BooleanValueObject altActionHappenedFlag) {
        if (altValue == null) {
            bindSimple(button, mainValue, stateLight);
            return;
        }

        final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
        final FunctionInfo primaryInfo = FunctionInfo.INFO1.get(button.getNoteAssign());
        final FunctionInfo alternateInfo = FunctionInfo.INFO2.get(button.getNoteAssign());
        mainValue.addValueObserver(active -> SequencerEditButtonLogic.handleValueChanged(
                active, primaryInfo, shiftActive.get(), feedback));
        altValue.addValueObserver(active -> SequencerEditButtonLogic.handleValueChanged(
                active, alternateInfo, shiftActive.get(), feedback));
        button.bindPressed(layer,
                pressed -> SequencerEditButtonLogic.handleAlternatePressed(
                        mainValue,
                        altValue,
                        alternateFunctionActive,
                        altActionHappenedFlag,
                        shiftActive.get(),
                        primaryInfo,
                        alternateInfo,
                        pressed,
                        feedback),
                () -> button.isPressed() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF);
        layer.bindLightState(() -> {
            final boolean active = button.isPressed() && !shiftActive.get();
            if (alternateFunctionActive.get()) {
                return active ? BiColorLightState.RED_FULL : BiColorLightState.RED_HALF;
            }
            return active ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }, stateLight);
    }

    private void bindSimple(final BiColorButton button,
                            final BooleanValueObject value,
                            final MultiStateHardwareLight stateLight) {
        final FunctionInfo info = FunctionInfo.INFO1.get(button.getNoteAssign());
        button.bind(layer, value, BiColorLightState.GREEN_FULL, BiColorLightState.OFF);
        layer.bindLightState(() -> BiColorLightState.AMBER_HALF, stateLight);
        value.addValueObserver(active -> SequencerEditButtonLogic.handleValueChanged(
                active, info, shiftActive.get(), feedback));
        layer.bindLightState(
                () -> button.isPressed() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF, stateLight);
    }

    public interface EditFunctionFeedback {
        void activate(FunctionInfo info, boolean shiftHeld);

        void deactivate();
    }
}
