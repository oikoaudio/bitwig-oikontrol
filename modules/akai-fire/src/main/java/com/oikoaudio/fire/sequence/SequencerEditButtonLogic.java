package com.oikoaudio.fire.sequence;

import com.bitwig.extensions.framework.values.BooleanValueObject;

public final class SequencerEditButtonLogic {
    private SequencerEditButtonLogic() {
    }

    static void handleAlternatePressed(final BooleanValueObject mainValue,
                                       final BooleanValueObject altValue,
                                       final BooleanValueObject alternateFunctionActive,
                                       final BooleanValueObject actionTakenFlag,
                                       final boolean shiftHeld,
                                       final FunctionInfo primaryInfo,
                                       final FunctionInfo alternateInfo,
                                       final boolean pressed,
                                       final SequencerEditButtonBinder.EditFunctionFeedback feedback) {
        if (pressed) {
            if (shiftHeld) {
                alternateFunctionActive.set(!alternateFunctionActive.get());
            } else {
                alternateFunctionActive.set(false);
            }
            final boolean alternateActive = alternateFunctionActive.get();
            mainValue.set(!alternateActive);
            altValue.set(alternateActive);
            actionTakenFlag.set(true);
            feedback.activate(alternateActive ? alternateInfo : primaryInfo, shiftHeld);
            return;
        }

        mainValue.set(false);
        if (!alternateFunctionActive.get()) {
            altValue.set(false);
        }
        actionTakenFlag.set(false);
        feedback.deactivate();
    }

    static void handleValueChanged(final boolean active,
                                   final FunctionInfo info,
                                   final boolean shiftHeld,
                                   final SequencerEditButtonBinder.EditFunctionFeedback feedback) {
        if (active) {
            feedback.activate(info, shiftHeld);
        } else {
            feedback.deactivate();
        }
    }
}
