package com.oikoaudio.fire;

import java.util.function.BooleanSupplier;

/** Owns the temporary track/device pins applied by standard drum mode. */
public final class DrumAutoPinController {
    public interface Port {
        boolean isTrackPinned();

        boolean isDevicePinned();

        int selectedTrackIndex();

        boolean focusFirstDrumMachine();

        void setTrackPinned(boolean pinned);

        void setDevicePinned(boolean pinned);

        boolean focusedDeviceExists();

        boolean focusedDeviceHasDrumPads();

        void restoreTrackSelection(int trackIndex);
    }

    private final BooleanSupplier eligible;
    private final Port port;

    private boolean applied;
    private boolean trackPinnedBeforeApply;
    private boolean devicePinnedBeforeApply;
    private int trackIndexBeforeApply = -1;

    public DrumAutoPinController(final BooleanSupplier eligible, final Port port) {
        this.eligible = eligible;
        this.port = port;
    }

    public boolean isApplied() {
        return applied;
    }

    public void sync() {
        if (eligible.getAsBoolean()) {
            applyIfEnabled();
        } else {
            release(true);
        }
    }

    public void applyIfEnabled() {
        if (!eligible.getAsBoolean() || applied) {
            return;
        }

        final boolean previousTrackPin = port.isTrackPinned();
        final boolean previousDevicePin = port.isDevicePinned();
        final int previousTrackIndex = port.selectedTrackIndex();
        if (!port.focusFirstDrumMachine()) {
            return;
        }

        trackPinnedBeforeApply = previousTrackPin;
        devicePinnedBeforeApply = previousDevicePin;
        trackIndexBeforeApply = previousTrackIndex;
        port.setTrackPinned(true);
        port.setDevicePinned(true);
        applied = true;
    }

    public void validate() {
        if (!eligible.getAsBoolean() || !applied) {
            return;
        }

        final boolean invalidPins = !port.isTrackPinned() || !port.isDevicePinned();
        final boolean invalidDevice =
                !port.focusedDeviceExists() || !port.focusedDeviceHasDrumPads();
        if (!invalidPins && !invalidDevice) {
            return;
        }

        if (port.focusFirstDrumMachine()) {
            port.setTrackPinned(true);
            port.setDevicePinned(true);
            return;
        }
        release(true);
    }

    public void release(final boolean restorePreviousState) {
        if (!applied) {
            return;
        }

        if (restorePreviousState) {
            port.setTrackPinned(trackPinnedBeforeApply);
            port.setDevicePinned(devicePinnedBeforeApply);
            port.restoreTrackSelection(trackIndexBeforeApply);
        } else {
            port.setTrackPinned(false);
            port.setDevicePinned(false);
        }
        applied = false;
        trackIndexBeforeApply = -1;
    }
}
