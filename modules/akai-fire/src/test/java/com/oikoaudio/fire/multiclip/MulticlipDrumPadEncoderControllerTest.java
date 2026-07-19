package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DeviceLayer;
import com.bitwig.extension.controller.api.DeviceLayerBank;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.oikoaudio.fire.display.OledDisplay;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MulticlipDrumPadEncoderControllerTest {
    @Test
    void remoteNavigationFallsBackToTheGroupInstrumentWithoutDrumPads() {
        final DrumPadBank pads = mock(DrumPadBank.class, Mockito.RETURNS_DEEP_STUBS);
        final DeviceLayerBank layers = mock(DeviceLayerBank.class, Mockito.RETURNS_DEEP_STUBS);
        final CursorRemoteControlsPage groupPage =
                mock(CursorRemoteControlsPage.class, Mockito.RETURNS_DEEP_STUBS);
        final AtomicBoolean hasDrumPads = new AtomicBoolean(false);
        final MulticlipDrumPadEncoderController controller =
                new MulticlipDrumPadEncoderController(
                        pads,
                        layers,
                        groupPage,
                        hasDrumPads::get,
                        () -> true,
                        mock(OledDisplay.class));

        assertSame(groupPage, controller.activeRemoteControlsPage());
    }

    @Test
    void mixerUsesTheActiveMaterializedGroupInstrumentChainWithoutDrumPads() {
        final DrumPadBank pads = mock(DrumPadBank.class, Mockito.RETURNS_DEEP_STUBS);
        final DeviceLayerBank layers = mock(DeviceLayerBank.class, Mockito.RETURNS_DEEP_STUBS);
        final DeviceLayer outputChain = mock(DeviceLayer.class, Mockito.RETURNS_DEEP_STUBS);
        when(layers.getItemAt(2)).thenReturn(outputChain);
        when(outputChain.exists().get()).thenReturn(true);
        when(outputChain.volume().exists().get()).thenReturn(true);
        final MulticlipDrumPadEncoderController controller =
                new MulticlipDrumPadEncoderController(
                        pads,
                        layers,
                        mock(CursorRemoteControlsPage.class, Mockito.RETURNS_DEEP_STUBS),
                        () -> false,
                        () -> true,
                        mock(OledDisplay.class));

        controller.setActivePad(2);

        assertTrue(controller.hasMixerParameter(0));
    }
}
