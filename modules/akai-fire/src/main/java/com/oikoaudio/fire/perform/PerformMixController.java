package com.oikoaudio.fire.perform;

import java.util.HashMap;
import java.util.Map;

/** Owns pure Mix gesture planning and remembered device selection by absolute track. */
public final class PerformMixController {
    private static final int DEVICE_ROWS = 4;
    private static final int DEVICE_PAGE_COUNT = 2;
    private static final int DEVICE_SLOTS = DEVICE_ROWS * DEVICE_PAGE_COUNT;

    public enum TrackAction {
        NONE,
        SELECT,
        STOP,
        SOLO,
        MUTE,
        ARM,
        ARM_EXCLUSIVE
    }

    public enum DeviceAction {
        SELECT,
        TOGGLE_ENABLED,
        TOGGLE_WINDOW
    }

    private final Map<Integer, Integer> rememberedDeviceByTrack = new HashMap<>();

    public static TrackAction trackAction(final int padIndex,
                                          final boolean altHeld,
                                          final boolean exclusiveArmEnabled) {
        final PerformPadRenderer.TrackAction row = PerformPadRenderer.TrackAction.fromPadIndex(padIndex);
        if (row == null) {
            return TrackAction.NONE;
        }
        return switch (row) {
            case SELECT -> altHeld ? TrackAction.STOP : TrackAction.SELECT;
            case SOLO -> TrackAction.SOLO;
            case MUTE -> TrackAction.MUTE;
            case ARM -> altHeld != exclusiveArmEnabled ? TrackAction.ARM_EXCLUSIVE : TrackAction.ARM;
        };
    }

    public static int deviceIndexForPad(final int padIndex, final int devicePageIndex) {
        if (padIndex < 0 || padIndex >= PerformLayout.PAD_COLUMNS * PerformLayout.PAD_ROWS) {
            return -1;
        }
        final int row = padIndex / PerformLayout.PAD_COLUMNS;
        final int page = Math.max(0, Math.min(DEVICE_PAGE_COUNT - 1, devicePageIndex));
        return page * DEVICE_ROWS + row;
    }

    public static int deviceIndexForRow(final int row, final int devicePageIndex) {
        if (row < 0 || row >= DEVICE_ROWS) {
            return -1;
        }
        final int page = Math.max(0, Math.min(DEVICE_PAGE_COUNT - 1, devicePageIndex));
        return page * DEVICE_ROWS + row;
    }

    public static DeviceAction deviceAction(final boolean altHeld, final boolean mainEncoderPressed) {
        if (altHeld) {
            return DeviceAction.TOGGLE_ENABLED;
        }
        return mainEncoderPressed ? DeviceAction.TOGGLE_WINDOW : DeviceAction.SELECT;
    }

    public static boolean rowWideToggleTarget(final boolean anyEnabled) {
        return !anyEnabled;
    }

    public void rememberDevice(final int absoluteTrackIndex, final int deviceIndex) {
        if (absoluteTrackIndex < 0 || deviceIndex < 0 || deviceIndex >= DEVICE_SLOTS) {
            return;
        }
        rememberedDeviceByTrack.put(absoluteTrackIndex, deviceIndex);
    }

    public int rememberedDevice(final int absoluteTrackIndex) {
        return absoluteTrackIndex < 0 ? -1 : rememberedDeviceByTrack.getOrDefault(absoluteTrackIndex, -1);
    }

    public void forgetDevice(final int absoluteTrackIndex) {
        rememberedDeviceByTrack.remove(absoluteTrackIndex);
    }
}
