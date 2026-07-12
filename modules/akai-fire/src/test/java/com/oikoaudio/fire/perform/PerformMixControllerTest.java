package com.oikoaudio.fire.perform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformMixControllerTest {
    @Test
    void resolvesEveryMixRowAndSelectAltStop() {
        assertEquals(PerformMixController.TrackAction.SELECT,
                PerformMixController.trackAction(0, false, false));
        assertEquals(PerformMixController.TrackAction.STOP,
                PerformMixController.trackAction(0, true, false));
        assertEquals(PerformMixController.TrackAction.SOLO,
                PerformMixController.trackAction(16, false, false));
        assertEquals(PerformMixController.TrackAction.MUTE,
                PerformMixController.trackAction(32, false, false));
        assertEquals(PerformMixController.TrackAction.ARM_EXCLUSIVE,
                PerformMixController.trackAction(48, true, false));
        assertEquals(PerformMixController.TrackAction.ARM,
                PerformMixController.trackAction(48, true, true));
    }

    @Test
    void remembersDevicesByAbsoluteTrackAndRejectsInvalidAddresses() {
        final PerformMixController controller = new PerformMixController();
        controller.rememberDevice(12, 3);
        controller.rememberDevice(-1, 2);
        controller.rememberDevice(4, -1);

        assertEquals(3, controller.rememberedDevice(12));
        assertEquals(-1, controller.rememberedDevice(4));
        controller.forgetDevice(12);
        assertEquals(-1, controller.rememberedDevice(12));
    }

    @Test
    void devicePagesAndGesturesPreserveExistingMapping() {
        assertEquals(6, PerformMixController.deviceIndexForPad(32, 1));
        assertEquals(PerformMixController.DeviceAction.TOGGLE_ENABLED,
                PerformMixController.deviceAction(true, false));
        assertEquals(PerformMixController.DeviceAction.TOGGLE_WINDOW,
                PerformMixController.deviceAction(false, true));
        assertEquals(PerformMixController.DeviceAction.SELECT,
                PerformMixController.deviceAction(false, false));
        assertEquals(false, PerformMixController.rowWideToggleTarget(true));
    }
}
