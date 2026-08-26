package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import org.junit.jupiter.api.Test;

class GlobalDeviceDeletionControllerTest {
    @Test
    void shiftAltPressDeletesSelectedDeviceAndConsumesRelease() {
        final PinnableCursorDevice selected = existingDevice();
        final GlobalDeviceDeletionController.Host host =
                mock(GlobalDeviceDeletionController.Host.class);
        when(host.shiftHeld()).thenReturn(true);
        when(host.altHeld()).thenReturn(true);
        when(host.selectedDevice()).thenReturn(selected);
        final GlobalDeviceDeletionController controller = new GlobalDeviceDeletionController(host);

        assertTrue(controller.handleMainEncoderPress(true));
        assertTrue(controller.handleMainEncoderPress(false));

        verify(selected).deleteObject();
    }

    @Test
    void altOnlyPressRemainsAvailableToToggleDeviceWindow() {
        final PinnableCursorDevice selected = existingDevice();
        final GlobalDeviceDeletionController.Host host =
                mock(GlobalDeviceDeletionController.Host.class);
        when(host.altHeld()).thenReturn(true);
        when(host.selectedDevice()).thenReturn(selected);
        final GlobalDeviceDeletionController controller = new GlobalDeviceDeletionController(host);

        assertFalse(controller.handleMainEncoderPress(true));

        verify(selected, never()).deleteObject();
    }

    @Test
    void reportsNoDeviceInsteadOfDeletingPrimaryDeviceWhenNothingIsSelected() {
        final PinnableCursorDevice selected = missingDevice();
        final PinnableCursorDevice primary = existingDevice();
        final GlobalDeviceDeletionController.Host host =
                mock(GlobalDeviceDeletionController.Host.class);
        when(host.shiftHeld()).thenReturn(true);
        when(host.altHeld()).thenReturn(true);
        when(host.selectedDevice()).thenReturn(selected);
        final GlobalDeviceDeletionController controller = new GlobalDeviceDeletionController(host);

        assertTrue(controller.handleMainEncoderPress(true));

        verify(primary, never()).deleteObject();
        verify(host).notifyAction("Delete Device", "No Device");
    }

    private static PinnableCursorDevice existingDevice() {
        final PinnableCursorDevice device = mock(PinnableCursorDevice.class);
        final BooleanValue exists = mock(BooleanValue.class);
        when(exists.get()).thenReturn(true);
        when(device.exists()).thenReturn(exists);
        return device;
    }

    private static PinnableCursorDevice missingDevice() {
        final PinnableCursorDevice device = mock(PinnableCursorDevice.class);
        final BooleanValue exists = mock(BooleanValue.class);
        when(exists.get()).thenReturn(false);
        when(device.exists()).thenReturn(exists);
        return device;
    }
}
