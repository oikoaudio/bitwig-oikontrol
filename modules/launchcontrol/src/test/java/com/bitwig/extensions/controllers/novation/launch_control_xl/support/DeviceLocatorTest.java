package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator.FocusResult;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeviceLocatorTest {
    private static final UUID ARPEGGIATOR_ID =
            UUID.fromString("4d407a2b-c91b-4e4c-9a89-c53c19fe6251");
    private static final UUID DRUM_MACHINE_ID =
            UUID.fromString("8ea97e45-0255-40fd-bc7e-94419741e9d1");

    @Test
    void findsArpeggiatorThroughABoundedMatcherFilteredBank() {
        final ControllerHost host = mock(ControllerHost.class);
        final TrackBank trackBank = mock(TrackBank.class);
        final Track track = mock(Track.class);
        final DeviceBank drumBank = mock(DeviceBank.class);
        final DeviceBank arpBank = mock(DeviceBank.class);
        final Device drumDevice = device(false, "");
        final Device arpDevice = device(true, "My renamed arp");
        final DeviceMatcher drumMatcher = mock(DeviceMatcher.class);
        final DeviceMatcher arpMatcher = mock(DeviceMatcher.class);

        when(host.createMainTrackBank(1, 0, 0)).thenReturn(trackBank);
        when(host.createBitwigDeviceMatcher(DRUM_MACHINE_ID)).thenReturn(drumMatcher);
        when(host.createBitwigDeviceMatcher(ARPEGGIATOR_ID)).thenReturn(arpMatcher);
        when(trackBank.getItemAt(0)).thenReturn(track);
        final BooleanValue trackExists = booleanValue(true);
        when(track.exists()).thenReturn(trackExists);
        when(track.createDeviceBank(1)).thenReturn(drumBank, arpBank);
        when(drumBank.getItemAt(0)).thenReturn(drumDevice);
        when(arpBank.getItemAt(0)).thenReturn(arpDevice);

        final DeviceLocator locator = new DeviceLocator(host, 1);
        final Optional<FocusResult> result = locator.focusFirst(Role.ARP);

        assertTrue(result.isPresent());
        assertSame(track, result.orElseThrow().track());
        assertSame(arpDevice, result.orElseThrow().device());
        verify(drumBank).setDeviceMatcher(drumMatcher);
        verify(arpBank).setDeviceMatcher(arpMatcher);
        verify(track, times(2)).createDeviceBank(1);
    }

    private static Device device(final boolean exists, final String name) {
        final Device device = mock(Device.class);
        final BooleanValue existsValue = booleanValue(exists);
        final StringValue nameValue = mock(StringValue.class);
        when(device.exists()).thenReturn(existsValue);
        when(device.name()).thenReturn(nameValue);
        when(nameValue.get()).thenReturn(name);
        return device;
    }

    private static BooleanValue booleanValue(final boolean value) {
        final BooleanValue result = mock(BooleanValue.class);
        when(result.get()).thenReturn(value);
        return result;
    }
}
