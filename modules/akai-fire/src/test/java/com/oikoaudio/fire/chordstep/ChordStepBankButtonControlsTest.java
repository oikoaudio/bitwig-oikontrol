package com.oikoaudio.fire.chordstep;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepBankButtonControlsTest {
    @Test
    void altPressAdjustsLengthWhenEnabled() {
        final Host host = new Host();
        host.alt = true;
        final ChordStepBankButtonControls controls = new ChordStepBankButtonControls(host);

        controls.handlePressed(true, 1, true);

        assertTrue(host.pendingLengthAdjust);
        assertEquals(1, host.lengthAdjust);
        assertEquals(1, host.clearPendingCount);
    }

    @Test
    void pressWithHeldStepsBeginsFineNudge() {
        final Host host = new Host();
        host.heldSteps = Set.of(2, 4);
        final ChordStepBankButtonControls controls = new ChordStepBankButtonControls(host);

        controls.handlePressed(true, -1, true);

        assertEquals(-1, host.nudgeAmount);
        assertEquals(Set.of(2, 4), host.nudgeSteps);
    }

    @Test
    void releaseShiftPressWithoutHeldStepsAdjustsPlayStartFine() {
        final Host host = new Host();
        host.shift = true;
        final ChordStepBankButtonControls controls = new ChordStepBankButtonControls(host);

        controls.handlePressed(true, 1, true);
        controls.handlePressed(false, 1, true);

        assertEquals(1, host.playStartAmount);
        assertTrue(host.playStartFine);
    }

    @Test
    void shiftBothBankButtonsSnapsPlayStartInsteadOfFineNudging() {
        final Host host = new Host();
        host.shift = true;
        final ChordStepBankButtonControls controls = new ChordStepBankButtonControls(host);

        controls.handlePressed(true, -1, true);
        controls.handlePressed(true, 1, true);
        controls.handlePressed(false, -1, true);
        controls.handlePressed(false, 1, true);

        assertEquals(1, host.snapPlayStartCount);
        assertEquals(0, host.playStartAmount);
    }

    @Test
    void releaseCompletesPendingFineNudgeBeforeClearing() {
        final Host host = new Host();
        host.completePending = true;
        final ChordStepBankButtonControls controls = new ChordStepBankButtonControls(host);

        controls.handlePressed(false, 1, true);

        assertEquals(1, host.completePendingCount);
        assertEquals(0, host.clearPendingCount);
    }

    @Test
    void releaseDuringMoveInFlightClearsPendingAction() {
        final Host host = new Host();
        host.moveInFlight = true;
        final ChordStepBankButtonControls controls = new ChordStepBankButtonControls(host);

        controls.handlePressed(false, 1, true);

        assertEquals(1, host.clearPendingCount);
    }

    private static final class Host implements ChordStepBankButtonControls.Host {
        private boolean alt;
        private boolean shift;
        private boolean pendingLengthAdjust;
        private boolean moveInFlight;
        private boolean completePending;
        private int lengthAdjust;
        private int nudgeAmount;
        private Set<Integer> heldSteps = Set.of();
        private Set<Integer> nudgeSteps = Set.of();
        private int playStartAmount;
        private boolean playStartFine;
        private int snapPlayStartCount;
        private int completePendingCount;
        private int clearPendingCount;

        @Override
        public boolean isAltHeld() {
            return alt;
        }

        @Override
        public boolean isShiftHeld() {
            return shift;
        }

        @Override
        public void setPendingLengthAdjust(final boolean pending) {
            pendingLengthAdjust = pending;
        }

        @Override
        public boolean isPendingLengthAdjust() {
            return pendingLengthAdjust;
        }

        @Override
        public void adjustLength(final int amount) {
            lengthAdjust = amount;
        }

        @Override
        public boolean isFineNudgeMoveInFlight() {
            return moveInFlight;
        }

        @Override
        public Set<Integer> heldStepSnapshot() {
            return heldSteps;
        }

        @Override
        public void beginHeldFineNudge(final int amount, final Set<Integer> heldSteps) {
            nudgeAmount = amount;
            nudgeSteps = heldSteps;
        }

        @Override
        public void adjustPlayStart(final int amount, final boolean fine) {
            playStartAmount = amount;
            playStartFine = fine;
        }

        @Override
        public void snapPlayStartToGrid() {
            snapPlayStartCount++;
        }

        @Override
        public boolean completePendingFineNudge() {
            completePendingCount++;
            return completePending;
        }

        @Override
        public void clearPendingBankAction() {
            clearPendingCount++;
        }
    }
}
