package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.values.SpecialDevices;

/**
 * Finds the first drum machine in the project and focuses it on the shared Fire cursors.
 */
public final class FireDeviceLocator {
    private final TrackBank trackBank;
    private final Device[] drumDevices;

    public FireDeviceLocator(final ControllerHost host, final int width) {
        trackBank = host.createMainTrackBank(width, 0, 0);
        drumDevices = new Device[width];

        final DeviceMatcher drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());
        for (int index = 0; index < width; index++) {
            final Track track = trackBank.getItemAt(index);
            track.exists().markInterested();

            final DeviceBank deviceBank = track.createDeviceBank(1);
            deviceBank.setDeviceMatcher(drumMatcher);
            final Device drumDevice = deviceBank.getItemAt(0);
            drumDevice.exists().markInterested();
            drumDevices[index] = drumDevice;
        }
    }

    public boolean focusFirstDrumMachine(final ViewCursorControl viewControl) {
        for (int index = 0; index < drumDevices.length; index++) {
            final Track track = trackBank.getItemAt(index);
            final Device drumDevice = drumDevices[index];
            if (track == null || !track.exists().get() || drumDevice == null || !drumDevice.exists().get()) {
                continue;
            }

            track.selectInMixer();
            track.selectInEditor();
            viewControl.getCursorTrack().selectChannel(track);
            viewControl.getPrimaryDevice().selectDevice(drumDevice);
            return true;
        }
        return false;
    }

}
