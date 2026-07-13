package com.bitwig.extensions.controllers.novation.launch_control_xl.factory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bitwig.extensions.controllers.novation.common.SimpleLedColor;
import org.junit.jupiter.api.Test;

class FactoryLedRendererTest {
    @Test
    void rendersTrackFocusAndMuteButtonsWithExistingColors() {
        final FactoryUiSnapshot.Strip[] strips = emptyStrips();
        strips[0] =
                FactoryUiSnapshot.Strip.existing(
                        true,
                        false,
                        false,
                        FactoryUiSnapshot.Value.missing(),
                        missingValues(),
                        missingValues(),
                        missingValues());
        final FactoryUiSnapshot state =
                snapshot(
                        FactoryUiSnapshot.Mode.SEND_2_DEVICE_1,
                        FactoryUiSnapshot.TrackControl.MUTE,
                        false,
                        0,
                        0,
                        strips);

        final FactoryLedRenderer.LedFrame frame = FactoryLedRenderer.render(state);

        assertEquals(SimpleLedColor.Amber.value(), frame.topButtons()[0]);
        assertEquals(SimpleLedColor.GreenLow.value(), frame.bottomButtons()[0]);
        assertEquals(SimpleLedColor.Off.value(), frame.topButtons()[1]);
    }

    @Test
    void rendersDevicePagesAndParameterLevels() {
        final FactoryUiSnapshot.Strip[] strips = emptyStrips();
        strips[0] =
                FactoryUiSnapshot.Strip.existing(
                        false,
                        false,
                        false,
                        FactoryUiSnapshot.Value.of(0.75),
                        new FactoryUiSnapshot.Value[] {
                            FactoryUiSnapshot.Value.of(0),
                            FactoryUiSnapshot.Value.of(0.25),
                            FactoryUiSnapshot.Value.missing()
                        },
                        new FactoryUiSnapshot.Value[] {
                            FactoryUiSnapshot.Value.of(0.75),
                            FactoryUiSnapshot.Value.missing(),
                            FactoryUiSnapshot.Value.missing()
                        },
                        missingValues());
        final FactoryUiSnapshot state =
                snapshot(
                        FactoryUiSnapshot.Mode.SEND_2_DEVICE_1,
                        FactoryUiSnapshot.TrackControl.NONE,
                        true,
                        0,
                        2,
                        strips);

        final FactoryLedRenderer.LedFrame frame = FactoryLedRenderer.render(state);

        assertEquals(SimpleLedColor.Amber.value(), frame.bottomButtons()[0]);
        assertEquals(SimpleLedColor.AmberLow.value(), frame.bottomButtons()[1]);
        assertEquals(SimpleLedColor.Off.value(), frame.knobs()[0]);
        assertEquals(SimpleLedColor.GreenLow.value(), frame.knobs()[8]);
        assertEquals(SimpleLedColor.Amber.value(), frame.knobs()[16]);
    }

    @Test
    void rendersFactoryRightButtonsForTrackAndDeviceNavigation() {
        final FactoryUiSnapshot track =
                snapshot(
                                FactoryUiSnapshot.Mode.SEND_3,
                                FactoryUiSnapshot.TrackControl.SOLO,
                                false,
                                0,
                                0,
                                emptyStrips())
                        .withNavigation(true, false, true, false, false, false);
        final FactoryUiSnapshot device =
                track.withDeviceOn(true).withNavigation(true, false, false, false, true, true);

        assertArrayEquals(
                new int[] {12, 12, 62, 12, 62, 12, 62, 12},
                FactoryLedRenderer.render(track).rightButtons());
        assertArrayEquals(
                new int[] {62, 12, 62, 12, 62, 12, 62, 62},
                FactoryLedRenderer.render(device).rightButtons());
    }

    @Test
    void renderedFramesDoNotExposeMutableLedArrays() {
        final FactoryLedRenderer.LedFrame frame =
                FactoryLedRenderer.render(
                        snapshot(
                                FactoryUiSnapshot.Mode.SEND_3,
                                FactoryUiSnapshot.TrackControl.NONE,
                                false,
                                0,
                                0,
                                emptyStrips()));

        frame.knobs()[0] = 99;
        frame.bottomButtons()[0] = 99;

        assertEquals(SimpleLedColor.Off.value(), frame.knobs()[0]);
        assertEquals(SimpleLedColor.Off.value(), frame.bottomButtons()[0]);
    }

    private static FactoryUiSnapshot snapshot(
            final FactoryUiSnapshot.Mode mode,
            final FactoryUiSnapshot.TrackControl control,
            final boolean deviceOn,
            final int page,
            final int pages,
            final FactoryUiSnapshot.Strip[] strips) {
        return new FactoryUiSnapshot(
                FactoryUiSnapshot.Surface.FACTORY,
                mode,
                control,
                deviceOn,
                page,
                pages,
                0,
                strips,
                missingValues(8),
                missingValues(8),
                false,
                false,
                false,
                false,
                false,
                false);
    }

    private static FactoryUiSnapshot.Strip[] emptyStrips() {
        final FactoryUiSnapshot.Strip[] strips = new FactoryUiSnapshot.Strip[8];
        for (int i = 0; i < strips.length; i++) strips[i] = FactoryUiSnapshot.Strip.missing();
        return strips;
    }

    private static FactoryUiSnapshot.Value[] missingValues() {
        return missingValues(3);
    }

    private static FactoryUiSnapshot.Value[] missingValues(final int count) {
        final FactoryUiSnapshot.Value[] values = new FactoryUiSnapshot.Value[count];
        for (int i = 0; i < count; i++) values[i] = FactoryUiSnapshot.Value.missing();
        return values;
    }
}
