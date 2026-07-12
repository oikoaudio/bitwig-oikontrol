package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.lights.RgbLigthState;

/** Pure colour rendering for Perform pads from bounded state snapshots. */
public final class PerformPadRenderer {
    private static final RgbLigthState SETTINGS_LOGO_ON = new RgbLigthState(127, 20, 0, true);
    private static final RgbLigthState BIRDS_EYE_AVAILABLE = new RgbLigthState(0, 36, 84, true);
    private static final RgbLigthState BIRDS_EYE_CURRENT = new RgbLigthState(0, 108, 127, true);
    private static final boolean[][] SETTINGS_LOGO = {
            {true, true, true, false, true, true, true, false, true, true, true, false, true, true, true, true},
            {true, false, false, false, false, true, false, false, true, false, true, false, true, false, false, false},
            {true, true, false, false, false, true, false, false, true, true, false, false, true, true, true, false},
            {true, false, false, false, true, true, true, false, true, false, true, false, true, true, true, true}
    };

    public enum TrackAction {
        SELECT(0, "Select", new RgbLigthState(0, 96, 96, true)),
        SOLO(1, "Solo", new RgbLigthState(96, 96, 0, true)),
        MUTE(2, "Mute", new RgbLigthState(110, 48, 0, true)),
        ARM(3, "Arm", new RgbLigthState(110, 0, 0, true));

        private final int rowIndex;
        private final String label;
        private final RgbLigthState color;

        TrackAction(final int rowIndex, final String label, final RgbLigthState color) {
            this.rowIndex = rowIndex;
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public RgbLigthState color() {
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

    public record SlotSnapshot(boolean exists,
                               boolean hasContent,
                               boolean selected,
                               boolean recording,
                               boolean recordingQueued,
                               boolean playbackQueued,
                               boolean stopQueued,
                               boolean playing,
                               RgbLigthState color) {
        public static SlotSnapshot missing() {
            return new SlotSnapshot(false, false, false, false, false, false, false, false, RgbLigthState.WHITE);
        }
    }

    public record SceneSnapshot(boolean exists,
                                RgbLigthState color,
                                boolean pending,
                                boolean selected,
                                boolean recording,
                                boolean playing) {
    }

    public record TrackSnapshot(TrackAction action,
                                boolean exists,
                                RgbLigthState color,
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

    public record MixDeviceSnapshot(boolean exists, RgbLigthState trackColor, boolean enabled, boolean selected) {
    }

    public record DeviceLayerSnapshot(TrackAction action,
                                      boolean exists,
                                      RgbLigthState color,
                                      boolean solo,
                                      boolean muted,
                                      boolean active) {
    }

    private PerformPadRenderer() {
    }

    public static RgbLigthState settingsLogo(final int padIndex) {
        if (padIndex < 0 || padIndex >= PerformLayout.PAD_COLUMNS * PerformLayout.PAD_ROWS) {
            return RgbLigthState.OFF;
        }
        final int row = padIndex / PerformLayout.PAD_COLUMNS;
        final int column = padIndex % PerformLayout.PAD_COLUMNS;
        return SETTINGS_LOGO[row][column] ? SETTINGS_LOGO_ON : RgbLigthState.OFF;
    }

    public static RgbLigthState slot(final SlotSnapshot snapshot, final int blinkTick) {
        if (!snapshot.exists()) {
            return RgbLigthState.OFF;
        }
        if (!snapshot.hasContent()) {
            return snapshot.selected() ? RgbLigthState.GRAY_2 : RgbLigthState.GRAY_1;
        }
        final RgbLigthState color = snapshot.color() == null ? RgbLigthState.WHITE : snapshot.color();
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

    public static RgbLigthState scene(final SceneSnapshot snapshot, final int blinkTick) {
        if (!snapshot.exists()) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState color = colorOr(snapshot.color(), RgbLigthState.PURPLE);
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

    public static RgbLigthState trackAction(final TrackSnapshot snapshot, final int blinkTick) {
        if (!snapshot.exists() || snapshot.action() == null) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState trackColor = colorOr(snapshot.color(), TrackAction.SELECT.color());
        return switch (snapshot.action()) {
            case SELECT -> snapshot.queuedForStop()
                    ? blinkFast(trackColor.getBrightest(), trackColor.getDimmed(), blinkTick)
                    : mixSelect(trackColor, snapshot.selected(), snapshot.stopped());
            case SOLO -> snapshot.solo() ? TrackAction.SOLO.color().getBrightest() : TrackAction.SOLO.color().getDimmed();
            case MUTE -> snapshot.muted() ? TrackAction.MUTE.color().getBrightest() : TrackAction.MUTE.color().getDimmed();
            case ARM -> snapshot.armed() ? TrackAction.ARM.color() : TrackAction.ARM.color().getDimmed();
        };
    }

    public static RgbLigthState mixDevice(final MixDeviceSnapshot snapshot) {
        if (!snapshot.exists()) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState color = colorOr(snapshot.trackColor(), TrackAction.SELECT.color());
        if (snapshot.selected() && snapshot.enabled()) {
            return color.getBrightest();
        }
        if (snapshot.selected()) {
            return color.getSoftDimmed();
        }
        return snapshot.enabled() ? color : color.getDimmed();
    }

    public static RgbLigthState deviceLayer(final DeviceLayerSnapshot snapshot) {
        if (!snapshot.exists() || snapshot.action() == null) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState color = colorOr(snapshot.color(), TrackAction.SELECT.color());
        if (!snapshot.active() && snapshot.action() != TrackAction.ARM) {
            return color.getSoftDimmed();
        }
        return switch (snapshot.action()) {
            case SELECT -> color;
            case SOLO -> snapshot.solo() ? TrackAction.SOLO.color() : RgbLigthState.OFF;
            case MUTE -> snapshot.muted() ? TrackAction.MUTE.color() : RgbLigthState.OFF;
            case ARM -> snapshot.active() ? TrackAction.ARM.color() : TrackAction.ARM.color().getSoftDimmed();
        };
    }

    public static RgbLigthState birdsEye(final boolean available, final boolean current) {
        if (!available) {
            return RgbLigthState.OFF;
        }
        return current ? BIRDS_EYE_CURRENT : BIRDS_EYE_AVAILABLE;
    }

    public static RgbLigthState mixSelect(final RgbLigthState trackColor,
                                          final boolean selected,
                                          final boolean stopped) {
        final RgbLigthState color = colorOr(trackColor, TrackAction.SELECT.color());
        if (selected) {
            return color.getBrightest();
        }
        return stopped ? color.getDimmed() : color;
    }

    private static RgbLigthState colorOr(final RgbLigthState color, final RgbLigthState fallback) {
        return color == null || RgbLigthState.OFF.equals(color) ? fallback : color;
    }

    private static RgbLigthState blinkSlow(final RgbLigthState on,
                                           final RgbLigthState off,
                                           final int blinkTick) {
        return blinkTick % 8 < 4 ? on : off;
    }

    private static RgbLigthState blinkFast(final RgbLigthState on,
                                           final RgbLigthState off,
                                           final int blinkTick) {
        return blinkTick % 2 == 0 ? on : off;
    }
}
