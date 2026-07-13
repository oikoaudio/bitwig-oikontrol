package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.lights.RgbLightState;

/** Pure colour rendering for Perform pads from bounded state snapshots. */
public final class PerformPadRenderer {
    private static final RgbLightState SETTINGS_LOGO_ON = new RgbLightState(127, 20, 0, true);
    private static final RgbLightState BIRDS_EYE_AVAILABLE = new RgbLightState(0, 36, 84, true);
    private static final RgbLightState BIRDS_EYE_CURRENT = new RgbLightState(0, 108, 127, true);
    private static final boolean[][] SETTINGS_LOGO = {
        {
            true, true, true, false, true, true, true, false, true, true, true, false, true, true,
            true, true
        },
        {
            true, false, false, false, false, true, false, false, true, false, true, false, true,
            false, false, false
        },
        {
            true, true, false, false, false, true, false, false, true, true, false, false, true,
            true, true, false
        },
        {
            true, false, false, false, true, true, true, false, true, false, true, false, true,
            true, true, true
        }
    };

    public enum TrackAction {
        SELECT(0, "Select", new RgbLightState(0, 96, 96, true)),
        SOLO(1, "Solo", new RgbLightState(96, 96, 0, true)),
        MUTE(2, "Mute", new RgbLightState(110, 48, 0, true)),
        ARM(3, "Arm", new RgbLightState(110, 0, 0, true));

        private final int rowIndex;
        private final String label;
        private final RgbLightState color;

        TrackAction(final int rowIndex, final String label, final RgbLightState color) {
            this.rowIndex = rowIndex;
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public RgbLightState color() {
            return color;
        }

        public static TrackAction fromPadIndex(final int padIndex) {
            if (padIndex < 0 || padIndex >= PerformLayout.PAD_COLUMNS * PerformLayout.PAD_ROWS) {
                return null;
            }
            final int row = padIndex / PerformLayout.PAD_COLUMNS;
            for (final TrackAction action : values()) {
                if (action.rowIndex == row) {
                    return action;
                }
            }
            return null;
        }
    }

    public record SlotSnapshot(
            boolean exists,
            boolean hasContent,
            boolean selected,
            boolean recording,
            boolean recordingQueued,
            boolean playbackQueued,
            boolean stopQueued,
            boolean playing,
            RgbLightState color) {
        public static SlotSnapshot missing() {
            return new SlotSnapshot(
                    false, false, false, false, false, false, false, false, RgbLightState.WHITE);
        }
    }

    public record SceneSnapshot(
            boolean exists,
            RgbLightState color,
            boolean pending,
            boolean selected,
            boolean recording,
            boolean playing) {}

    public record TrackSnapshot(
            TrackAction action,
            boolean exists,
            RgbLightState color,
            boolean selected,
            boolean stopped,
            boolean queuedForStop,
            boolean solo,
            boolean muted,
            boolean armed) {
        public static TrackSnapshot missing(final TrackAction action) {
            return new TrackSnapshot(action, false, null, false, false, false, false, false, false);
        }
    }

    public record MixDeviceSnapshot(
            boolean exists, RgbLightState trackColor, boolean enabled, boolean selected) {}

    public record DeviceLayerSnapshot(
            TrackAction action,
            boolean exists,
            RgbLightState color,
            boolean solo,
            boolean muted,
            boolean active) {}

    private PerformPadRenderer() {}

    public static RgbLightState settingsLogo(final int padIndex) {
        if (padIndex < 0 || padIndex >= PerformLayout.PAD_COLUMNS * PerformLayout.PAD_ROWS) {
            return RgbLightState.OFF;
        }
        final int row = padIndex / PerformLayout.PAD_COLUMNS;
        final int column = padIndex % PerformLayout.PAD_COLUMNS;
        return SETTINGS_LOGO[row][column] ? SETTINGS_LOGO_ON : RgbLightState.OFF;
    }

    public static RgbLightState slot(final SlotSnapshot snapshot, final int blinkTick) {
        if (!snapshot.exists()) {
            return RgbLightState.OFF;
        }
        if (!snapshot.hasContent()) {
            return snapshot.selected() ? RgbLightState.GRAY_2 : RgbLightState.GRAY_1;
        }
        final RgbLightState color =
                snapshot.color() == null ? RgbLightState.WHITE : snapshot.color();
        if (snapshot.recording()) {
            return blinkFast(color.getBrightest(), color, blinkTick);
        }
        if (snapshot.recordingQueued()) {
            return blinkFast(color.getBrightest(), color.getDimmed(), blinkTick);
        }
        if (snapshot.playbackQueued() || snapshot.stopQueued()) {
            return blinkFast(color.getBrightend(), color.getDimmed(), blinkTick);
        }
        if (snapshot.playing()) {
            return snapshot.selected()
                    ? blinkSlow(color.getBrightest(), color, blinkTick)
                    : blinkSlow(color, color.getDimmed(), blinkTick);
        }
        return snapshot.selected() ? color.getBrightend() : color.getSoftDimmed();
    }

    public static RgbLightState scene(final SceneSnapshot snapshot, final int blinkTick) {
        if (!snapshot.exists()) {
            return RgbLightState.OFF;
        }
        final RgbLightState color = colorOr(snapshot.color(), RgbLightState.PURPLE);
        if (snapshot.pending()) {
            return blinkFast(color.getBrightest(), color.getDimmed(), blinkTick);
        }
        if (snapshot.selected()) {
            return color.getBrightest();
        }
        if (snapshot.recording()) {
            return blinkFast(color.getBrightest(), color, blinkTick);
        }
        if (snapshot.playing()) {
            return blinkSlow(color, color.getDimmed(), blinkTick);
        }
        return color.getSoftDimmed();
    }

    public static RgbLightState trackAction(final TrackSnapshot snapshot, final int blinkTick) {
        if (!snapshot.exists() || snapshot.action() == null) {
            return RgbLightState.OFF;
        }
        final RgbLightState trackColor = colorOr(snapshot.color(), TrackAction.SELECT.color());
        return switch (snapshot.action()) {
            case SELECT ->
                    snapshot.queuedForStop()
                            ? blinkFast(
                                    trackColor.getBrightest(), trackColor.getDimmed(), blinkTick)
                            : mixSelect(trackColor, snapshot.selected(), snapshot.stopped());
            case SOLO ->
                    snapshot.solo()
                            ? TrackAction.SOLO.color().getBrightest()
                            : TrackAction.SOLO.color().getDimmed();
            case MUTE ->
                    snapshot.muted()
                            ? TrackAction.MUTE.color().getBrightest()
                            : TrackAction.MUTE.color().getDimmed();
            case ARM ->
                    snapshot.armed()
                            ? TrackAction.ARM.color()
                            : TrackAction.ARM.color().getDimmed();
        };
    }

    public static RgbLightState mixDevice(final MixDeviceSnapshot snapshot) {
        if (!snapshot.exists()) {
            return RgbLightState.OFF;
        }
        final RgbLightState color = colorOr(snapshot.trackColor(), TrackAction.SELECT.color());
        if (snapshot.selected() && snapshot.enabled()) {
            return color.getBrightest();
        }
        if (snapshot.selected()) {
            return color.getSoftDimmed();
        }
        return snapshot.enabled() ? color : color.getDimmed();
    }

    public static RgbLightState deviceLayer(final DeviceLayerSnapshot snapshot) {
        if (!snapshot.exists() || snapshot.action() == null) {
            return RgbLightState.OFF;
        }
        final RgbLightState color = colorOr(snapshot.color(), TrackAction.SELECT.color());
        if (!snapshot.active() && snapshot.action() != TrackAction.ARM) {
            return color.getSoftDimmed();
        }
        return switch (snapshot.action()) {
            case SELECT -> color;
            case SOLO -> snapshot.solo() ? TrackAction.SOLO.color() : RgbLightState.OFF;
            case MUTE -> snapshot.muted() ? TrackAction.MUTE.color() : RgbLightState.OFF;
            case ARM ->
                    snapshot.active()
                            ? TrackAction.ARM.color()
                            : TrackAction.ARM.color().getSoftDimmed();
        };
    }

    public static RgbLightState birdsEye(final boolean available, final boolean current) {
        if (!available) {
            return RgbLightState.OFF;
        }
        return current ? BIRDS_EYE_CURRENT : BIRDS_EYE_AVAILABLE;
    }

    public static RgbLightState mixSelect(
            final RgbLightState trackColor, final boolean selected, final boolean stopped) {
        final RgbLightState color = colorOr(trackColor, TrackAction.SELECT.color());
        if (selected) {
            return color.getBrightest();
        }
        return stopped ? color.getDimmed() : color;
    }

    private static RgbLightState colorOr(final RgbLightState color, final RgbLightState fallback) {
        return color == null || RgbLightState.OFF.equals(color) ? fallback : color;
    }

    private static RgbLightState blinkSlow(
            final RgbLightState on, final RgbLightState off, final int blinkTick) {
        return blinkTick % 8 < 4 ? on : off;
    }

    private static RgbLightState blinkFast(
            final RgbLightState on, final RgbLightState off, final int blinkTick) {
        return blinkTick % 2 == 0 ? on : off;
    }
}
