package com.bitwig.extensions.controllers.novation.launch_control_xl.factory;

import java.util.Objects;

/**
 * Owns factory-template interaction state and policy independently of Bitwig and hardware objects.
 * The extension remains responsible for constructing layers and adapting this controller's narrow
 * port.
 */
public final class FactoryLayerController {
    private final Port port;
    private final int stripCount;
    private FactoryUiSnapshot.TrackControl trackControl = FactoryUiSnapshot.TrackControl.NONE;
    private boolean deviceOn;
    private boolean exclusiveTrackArmEnabled;

    public FactoryLayerController(final Port port, final int stripCount) {
        this.port = Objects.requireNonNull(port);
        if (stripCount <= 0) {
            throw new IllegalArgumentException("stripCount must be positive");
        }
        this.stripCount = stripCount;
    }

    public FactoryUiSnapshot.TrackControl trackControl() {
        return trackControl;
    }

    public boolean deviceOn() {
        return deviceOn;
    }

    public void setExclusiveTrackArmEnabled(final boolean enabled) {
        exclusiveTrackArmEnabled = enabled;
    }

    public void setTrackControl(final FactoryUiSnapshot.TrackControl control) {
        trackControl = Objects.requireNonNull(control);
        port.applyTrackControl(control);
    }

    public void toggleTrackControl(final FactoryUiSnapshot.TrackControl desired) {
        setTrackControl(trackControl == desired ? FactoryUiSnapshot.TrackControl.NONE : desired);
    }

    public void setDeviceOn(final boolean enabled) {
        deviceOn = enabled;
        port.applyDeviceOn(enabled);
    }

    public void selectMode(final FactoryUiSnapshot.Mode selectedMode) {
        Objects.requireNonNull(selectedMode);
        port.applyMode(selectedMode);
        switch (selectedMode) {
            case SEND_2_FULL_DEVICE, SEND_2_PROJECT, SEND_2_DEVICE_1, SEND_2_PAN_1 ->
                    port.setSendBankSize(2);
            case SEND_3 -> port.setSendBankSize(3);
            case SEND_1_DEVICE_2 -> port.setSendBankSize(1);
            default -> {}
        }
    }

    public void setTrackValue(final int strip, final TrackValueTarget target, final double value) {
        if (isControllable(strip)) {
            port.setTrackValue(strip, target, value);
        }
    }

    public void setSendValue(final int strip, final int send, final double value) {
        if (isControllable(strip)) {
            port.setSendValue(strip, send, value);
        }
    }

    public void setDeviceRemoteValue(final int strip, final int parameter, final double value) {
        if (isControllable(strip)) {
            port.setDeviceRemoteValue(strip, parameter, value);
        }
    }

    public void setTrackRemoteValue(final int strip, final int parameter, final double value) {
        if (isControllable(strip)) {
            port.setTrackRemoteValue(strip, parameter, value);
        }
    }

    public void setSelectedDeviceRemoteValue(final int parameter, final double value) {
        port.setSelectedDeviceRemoteValue(parameter, value);
    }

    public void setProjectRemoteValue(final int parameter, final double value) {
        port.setProjectRemoteValue(parameter, value);
    }

    public void toggleTrackRemoteButton(final int strip) {
        if (isControllable(strip)) {
            port.toggleTrackRemoteButton(strip);
        }
    }

    public void selectDeviceRemotePage(final int page) {
        port.selectDeviceRemotePage(page);
    }

    public void selectPreviousDevice() {
        port.selectPreviousDevice();
    }

    public void selectNextDevice() {
        port.selectNextDevice();
    }

    public void selectTrack(final int strip) {
        if (isControllable(strip)) {
            port.selectTrack(strip);
        }
    }

    public void scrollAllSendBanks(final int direction) {
        if (direction == 0) {
            return;
        }
        for (int strip = 0; strip < stripCount; strip++) {
            if (isControllable(strip)) {
                port.scrollSendBank(strip, direction);
            }
        }
    }

    public void scrollTrackPage(final int direction) {
        if (direction == 0 || !port.canScrollTrackPage(direction)) {
            return;
        }
        port.scrollTrackPage(direction);
        port.scheduleFirstTrackSelection();
    }

    public void toggleTrackBoolean(final int strip, final TrackBooleanTarget target) {
        if (!isControllable(strip)) {
            return;
        }
        switch (target) {
            case MUTE -> port.toggleTrackMute(strip);
            case SOLO -> port.toggleTrackSolo(strip);
            case ARM -> toggleTrackArm(strip);
        }
    }

    private void toggleTrackArm(final int strip) {
        if (!exclusiveTrackArmEnabled) {
            port.setTrackArm(strip, !port.trackArm(strip));
            return;
        }
        final boolean shouldArm = !port.trackArm(strip);
        if (shouldArm) {
            for (int other = 0; other < stripCount; other++) {
                if (other != strip && isControllable(other)) {
                    port.setTrackArm(other, false);
                }
            }
        }
        port.setTrackArm(strip, shouldArm);
        port.selectTrack(strip);
    }

    private boolean isControllable(final int strip) {
        return strip >= 0 && strip < stripCount && port.trackExists(strip);
    }

    public enum TrackBooleanTarget {
        MUTE,
        SOLO,
        ARM
    }

    public enum TrackValueTarget {
        VOLUME,
        PAN
    }

    public interface Port {
        boolean trackExists(int strip);

        boolean trackArm(int strip);

        void setTrackArm(int strip, boolean value);

        void toggleTrackMute(int strip);

        void toggleTrackSolo(int strip);

        void selectTrack(int strip);

        void scrollSendBank(int strip, int direction);

        boolean canScrollTrackPage(int direction);

        void scrollTrackPage(int direction);

        void scheduleFirstTrackSelection();

        void setTrackValue(int strip, TrackValueTarget target, double value);

        void setSendValue(int strip, int send, double value);

        void setDeviceRemoteValue(int strip, int parameter, double value);

        void setTrackRemoteValue(int strip, int parameter, double value);

        void setSelectedDeviceRemoteValue(int parameter, double value);

        void setProjectRemoteValue(int parameter, double value);

        void toggleTrackRemoteButton(int strip);

        void selectDeviceRemotePage(int page);

        void selectPreviousDevice();

        void selectNextDevice();

        void applyMode(FactoryUiSnapshot.Mode mode);

        void setSendBankSize(int size);

        void applyTrackControl(FactoryUiSnapshot.TrackControl control);

        void applyDeviceOn(boolean deviceOn);
    }
}
