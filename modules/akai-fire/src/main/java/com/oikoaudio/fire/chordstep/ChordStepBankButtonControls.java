package com.oikoaudio.fire.chordstep;

import java.util.Set;

/**
 * Owns chord-step behavior for the physical BANK buttons.
 */
public final class ChordStepBankButtonControls {
    private final Host host;

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

        boolean completePendingFineNudge();

        void clearPendingBankAction();
    }
}
