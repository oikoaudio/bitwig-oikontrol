package com.oikoaudio.fire.chordstep;

import java.util.Set;

/**
 * Owns chord-step behavior for the physical BANK buttons.
 */
public final class ChordStepBankButtonControls {
    private final Host host;
    private boolean leftHeld = false;
    private boolean rightHeld = false;
    private boolean shiftSnapConsumed = false;

    public ChordStepBankButtonControls(final Host host) {
        this.host = host;
    }

    public void handlePressed(final boolean pressed, final int amount, final boolean lengthAdjustEnabled) {
        if (lengthAdjustEnabled && host.isAltHeld()) {
            if (pressed) {
                host.setPendingLengthAdjust(true);
                host.adjustLength(amount);
            } else if (host.isPendingLengthAdjust()) {
                host.setPendingLengthAdjust(false);
            }
            host.clearPendingBankAction();
            return;
        }
        if (host.isFineNudgeMoveInFlight()) {
            if (!pressed) {
                host.clearPendingBankAction();
            }
            return;
        }
        if (host.isShiftHeld() && host.heldStepSnapshot().isEmpty()) {
            handleShiftClipStartButton(pressed, amount);
            return;
        }
        if (pressed) {
            final Set<Integer> heldSteps = host.heldStepSnapshot();
            if (!heldSteps.isEmpty()) {
                host.beginHeldFineNudge(amount, heldSteps);
            } else {
                host.adjustPlayStart(amount, host.isShiftHeld());
            }
            return;
        }
        if (host.completePendingFineNudge()) {
            return;
        }
        host.clearPendingBankAction();
    }

    private void handleShiftClipStartButton(final boolean pressed, final int amount) {
        if (pressed) {
            setHeld(amount, true);
            if (leftHeld && rightHeld) {
                host.snapPlayStartToGrid();
                shiftSnapConsumed = true;
            }
            return;
        }
        setHeld(amount, false);
        if (!shiftSnapConsumed) {
            host.adjustPlayStart(amount, true);
        }
        if (!leftHeld && !rightHeld) {
            shiftSnapConsumed = false;
        }
    }

    private void setHeld(final int amount, final boolean held) {
        if (amount < 0) {
            leftHeld = held;
        } else {
            rightHeld = held;
        }
    }

    public interface Host {
        boolean isAltHeld();

        boolean isShiftHeld();

        void setPendingLengthAdjust(boolean pending);

        boolean isPendingLengthAdjust();

        void adjustLength(int amount);

        boolean isFineNudgeMoveInFlight();

        Set<Integer> heldStepSnapshot();

        void beginHeldFineNudge(int amount, Set<Integer> heldSteps);

        void adjustPlayStart(int amount, boolean fine);

        void snapPlayStartToGrid();

        boolean completePendingFineNudge();

        void clearPendingBankAction();
    }
}
