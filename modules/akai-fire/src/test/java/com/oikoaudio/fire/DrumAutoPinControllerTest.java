package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DrumAutoPinControllerTest {
    private static final class FakePort implements DrumAutoPinController.Port {
        private boolean trackPinned;
        private boolean devicePinned;
        private int trackIndex = 3;
        private boolean focusSucceeds = true;
        private boolean deviceExists = true;
        private boolean deviceHasDrumPads = true;
        private int restoredTrackIndex = -1;

        @Override
        public boolean isTrackPinned() {
            return trackPinned;
        }

        @Override
        public boolean isDevicePinned() {
            return devicePinned;
        }

        @Override
        public int selectedTrackIndex() {
            return trackIndex;
        }

        @Override
        public boolean focusFirstDrumMachine() {
            return focusSucceeds;
        }

        @Override
        public void setTrackPinned(final boolean pinned) {
            trackPinned = pinned;
        }

        @Override
        public void setDevicePinned(final boolean pinned) {
            devicePinned = pinned;
        }

        @Override
        public boolean focusedDeviceExists() {
            return deviceExists;
        }

        @Override
        public boolean focusedDeviceHasDrumPads() {
            return deviceHasDrumPads;
        }

        @Override
        public void restoreTrackSelection(final int trackIndex) {
            restoredTrackIndex = trackIndex;
        }
    }

    @Test
    void appliesOnceAndRestoresTheExactPreviousState() {
        final FakePort port = new FakePort();
        port.trackPinned = true;
        final DrumAutoPinController controller = new DrumAutoPinController(() -> true, port);

        controller.applyIfEnabled();
        assertTrue(controller.isApplied());
        assertTrue(port.trackPinned);
        assertTrue(port.devicePinned);

        controller.release(true);
        assertFalse(controller.isApplied());
        assertTrue(port.trackPinned);
        assertFalse(port.devicePinned);
        assertEquals(3, port.restoredTrackIndex);
    }

    @Test
    void failedInitialFocusDoesNotOwnOrChangePinState() {
        final FakePort port = new FakePort();
        port.focusSucceeds = false;
        final DrumAutoPinController controller = new DrumAutoPinController(() -> true, port);

        controller.applyIfEnabled();

        assertFalse(controller.isApplied());
        assertFalse(port.trackPinned);
        assertFalse(port.devicePinned);
    }

    @Test
    void disappearingTargetReleasesStaleOwnershipAndRestoresState() {
        final FakePort port = new FakePort();
        final DrumAutoPinController controller = new DrumAutoPinController(() -> true, port);
        controller.applyIfEnabled();
        port.deviceExists = false;
        port.focusSucceeds = false;

        controller.validate();

        assertFalse(controller.isApplied());
        assertFalse(port.trackPinned);
        assertFalse(port.devicePinned);
        assertEquals(3, port.restoredTrackIndex);
    }

    @Test
    void syncReleasesWhenModeIsNoLongerEligible() {
        final FakePort port = new FakePort();
        final boolean[] eligible = {true};
        final DrumAutoPinController controller = new DrumAutoPinController(() -> eligible[0], port);
        controller.sync();
        eligible[0] = false;

        controller.sync();

        assertFalse(controller.isApplied());
        assertEquals(3, port.restoredTrackIndex);
    }
}
