package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.InsertionPoint;
import org.junit.jupiter.api.Test;

class PadContainerBrowserInsertionTest {
    @Test
    void replacesSelectedDeviceOnAnOccupiedPad() {
        final DrumPad pad = mock(DrumPad.class);
        final Device selectedDevice = mock(Device.class);
        final InsertionPoint replacement = mock(InsertionPoint.class);
        org.mockito.Mockito.when(selectedDevice.replaceDeviceInsertionPoint())
                .thenReturn(replacement);

        final InsertionPoint insertion =
                PadContainer.browserInsertionPoint(pad, selectedDevice, true);

        assertSame(replacement, insertion);
        verify(selectedDevice).replaceDeviceInsertionPoint();
    }

    @Test
    void usesPadInsertionPointWhenItsDeviceChainIsEmpty() {
        final DrumPad pad = mock(DrumPad.class);
        final Device selectedDevice = mock(Device.class);
        final InsertionPoint padInsertion = mock(InsertionPoint.class);
        org.mockito.Mockito.when(pad.insertionPoint()).thenReturn(padInsertion);

        final InsertionPoint insertion =
                PadContainer.browserInsertionPoint(pad, selectedDevice, false);

        assertSame(padInsertion, insertion);
        verify(pad).insertionPoint();
    }
}
