package com.oikoaudio.fire.sequence;

final class SeqClipRowActionResolver {
    enum Action {
        IGNORE,
        DELETE_OBJECT,
        CLEAR_STEPS,
        COPY_TO_TARGET,
        SELECT_ONLY,
        CYCLE_COLOR,
        SELECT_AND_LAUNCH,
        CREATE_EMPTY
    }

    private SeqClipRowActionResolver() {
    }

    static Action resolve(final boolean pressed, final boolean hasContent,
                          final boolean deleteHeld, final boolean copyHeld,
                          final boolean selectHeld, final boolean shiftHeld,
                          final int selectedSlotIndex, final int index) {
        if (!pressed) {
            return Action.IGNORE;
        }
        if (hasContent) {
            if (deleteHeld) {
                return shiftHeld ? Action.DELETE_OBJECT : Action.CLEAR_STEPS;
            }
            if (copyHeld) {
                return selectedSlotIndex != -1 && selectedSlotIndex != index
                        ? Action.COPY_TO_TARGET
                        : Action.IGNORE;
            }
            if (selectHeld) {
                return Action.SELECT_ONLY;
            }
            if (shiftHeld) {
                return Action.CYCLE_COLOR;
            }
            return Action.SELECT_AND_LAUNCH;
        }
        if (copyHeld && selectedSlotIndex != -1 && selectedSlotIndex != index) {
            return Action.COPY_TO_TARGET;
        }
        return Action.CREATE_EMPTY;
    }
}
