package com.oikoaudio.fire.perform;

/** Pure gesture planning for Perform launcher, Scene Launch, Birds-Eye, and bank navigation. */
public final class PerformLauncherNavigationController {
    public enum Route {
        LAUNCHER,
        SCENE,
        BIRDS_EYE,
        MIX
    }

    public enum SlotAction {
        NONE,
        SETTINGS,
        RECORD,
        DELETE,
        COPY,
        SELECT,
        STOP_RECORDING,
        LAUNCH,
        CREATE
    }

    public enum SceneAction {
        NONE,
        DELETE,
        COPY,
        SELECT,
        LAUNCH
    }

    public record SlotInput(boolean pressed,
                            boolean trackExists,
                            boolean slotExists,
                            boolean settingsHeld,
                            boolean recordTargetingHeld,
                            boolean deleteHeld,
                            boolean copyHeld,
                            boolean selectHeld,
                            boolean recording,
                            boolean hasContent) {
    }

    public record BirdsEyeJump(boolean available, int trackOffset, int sceneOffset) {
    }

    private PerformLauncherNavigationController() {
    }

    public static Route route(final PerformPageState state) {
        if (state.isTrackActionMode()) {
            return Route.MIX;
        }
        if (state.isBirdsEye()) {
            return Route.BIRDS_EYE;
        }
        if (state.isSceneLaunch()) {
            return Route.SCENE;
        }
        return Route.LAUNCHER;
    }

    public static SlotAction slotAction(final SlotInput input) {
        if (!input.pressed() || !input.trackExists() || !input.slotExists()) {
            return SlotAction.NONE;
        }
        if (input.settingsHeld()) {
            return SlotAction.SETTINGS;
        }
        if (input.recordTargetingHeld()) {
            return SlotAction.RECORD;
        }
        if (input.deleteHeld()) {
            return SlotAction.DELETE;
        }
        if (input.copyHeld()) {
            return SlotAction.COPY;
        }
        if (input.selectHeld()) {
            return SlotAction.SELECT;
        }
        if (input.recording()) {
            return SlotAction.STOP_RECORDING;
        }
        return input.hasContent() ? SlotAction.LAUNCH : SlotAction.CREATE;
    }

    public static SceneAction sceneAction(final boolean pressed,
                                          final boolean available,
                                          final boolean deleteHeld,
                                          final boolean copyHeld,
                                          final boolean selectHeld) {
        if (!pressed || !available) {
            return SceneAction.NONE;
        }
        if (deleteHeld) {
            return SceneAction.DELETE;
        }
        if (copyHeld) {
            return SceneAction.COPY;
        }
        if (selectHeld) {
            return SceneAction.SELECT;
        }
        return SceneAction.LAUNCH;
    }

    public static int nextOffset(final int current,
                                 final int totalCount,
                                 final int visibleCount,
                                 final int direction) {
        return nextOffsetBy(current, totalCount, visibleCount, direction, visibleCount);
    }

    public static int nextOffsetBy(final int current,
                                   final int totalCount,
                                   final int visibleCount,
                                   final int direction,
                                   final int amount) {
        final int maxOffset = Math.max(0, totalCount - Math.max(1, visibleCount));
        return clamp(current + direction * Math.max(1, amount), 0, maxOffset);
    }

    public static BirdsEyeJump birdsEyeJump(final int padIndex,
                                            final PerformLayout layout,
                                            final int totalTrackCount,
                                            final int totalSceneCount) {
        if (padIndex < 0 || padIndex >= PerformLayout.PAD_COLUMNS * PerformLayout.PAD_ROWS) {
            return new BirdsEyeJump(false, 0, 0);
        }
        final int trackBlock = layout.visibleTrackIndexForPad(padIndex) * layout.visibleTrackCount();
        final int sceneBlock = layout.visibleSceneIndexForPad(padIndex) * layout.visibleSceneCount();
        if (trackBlock >= totalTrackCount || sceneBlock >= totalSceneCount) {
            return new BirdsEyeJump(false, 0, 0);
        }
        return new BirdsEyeJump(true,
                clamp(trackBlock, 0, layout.maxTrackOffset(totalTrackCount)),
                clamp(sceneBlock, 0, layout.maxSceneOffset(totalSceneCount)));
    }

    private static int clamp(final int value, final int minimum, final int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
