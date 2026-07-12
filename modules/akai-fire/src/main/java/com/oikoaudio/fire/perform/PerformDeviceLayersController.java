package com.oikoaudio.fire.perform;

/** Pure gesture planning for the Device Layers page. */
public final class PerformDeviceLayersController {
    public enum Action {
        NONE,
        SELECT,
        SOLO,
        MUTE,
        TOGGLE_ACTIVE
    }

    private PerformDeviceLayersController() {
    }

    public static Action actionForPad(final int padIndex) {
        final PerformPadRenderer.TrackAction row = PerformPadRenderer.TrackAction.fromPadIndex(padIndex);
        if (row == null) {
            return Action.NONE;
        }
        return switch (row) {
            case SELECT -> Action.SELECT;
            case SOLO -> Action.SOLO;
            case MUTE -> Action.MUTE;
            case ARM -> Action.TOGGLE_ACTIVE;
        };
    }
}
