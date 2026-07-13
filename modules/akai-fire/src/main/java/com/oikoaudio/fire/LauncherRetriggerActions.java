package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import java.util.Locale;

final class LauncherRetriggerActions {
    static final String DEFAULT_LAUNCH_QUANTIZATION = "default";
    static final String FROM_START_LAUNCH_MODE = "from_start";

    private static final String ACTION_RETRIGGER_PLAYING_LAUNCHER_CLIPS_ID =
            "retrigger_playing_launcher_clips";
    private static final String ACTION_RETRIGGER_PLAYING_LAUNCHER_CLIPS_LABEL =
            "Retrigger playing Launcher clips";
    private static final String NORMALIZED_RETRIGGER_PLAYING_LAUNCHER_CLIPS_LABEL =
            "retrigger playing launcher clips";

    private LauncherRetriggerActions() {}

    /**
     * Invokes Bitwig's project-level launcher retrigger action when the host exposes it.
     *
     * @return true when Bitwig accepted a global retrigger action, false when callers should fall
     *     back.
     */
    static boolean retriggerPlayingLauncherClips(final Application application) {
        final Action action = resolveRetriggerPlayingLauncherClipsAction(application);
        if (action == null) {
            return false;
        }
        action.invoke();
        return true;
    }

    /**
     * Fallback for older or differently named Bitwig actions: retrigger only the selected launcher
     * clip.
     */
    static void retriggerCurrentClip(final PinnableCursorClip selectedClip) {
        if (selectedClip != null) {
            selectedClip.launchWithOptions(DEFAULT_LAUNCH_QUANTIZATION, FROM_START_LAUNCH_MODE);
        }
    }

    /**
     * Resolve the global action defensively because Bitwig actions are string-addressed and their
     * ids are less stable than typed API methods. Try the expected id first, then the visible GUI
     * label, then scan all exposed actions for a matching id/name/menu label.
     */
    static Action resolveRetriggerPlayingLauncherClipsAction(final Application application) {
        if (application == null) {
            return null;
        }
        Action action = application.getAction(ACTION_RETRIGGER_PLAYING_LAUNCHER_CLIPS_ID);
        if (action != null) {
            return action;
        }
        action = application.getAction(ACTION_RETRIGGER_PLAYING_LAUNCHER_CLIPS_LABEL);
        if (action != null) {
            return action;
        }
        final Action[] actions = application.getActions();
        if (actions == null) {
            return null;
        }
        for (final Action candidate : actions) {
            if (isRetriggerPlayingLauncherClipsAction(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isRetriggerPlayingLauncherClipsAction(final Action action) {
        return action != null
                && (isRetriggerPlayingLauncherClipsText(action.getId())
                        || isRetriggerPlayingLauncherClipsText(action.getName())
                        || isRetriggerPlayingLauncherClipsText(action.getMenuItemText()));
    }

    private static boolean isRetriggerPlayingLauncherClipsText(final String text) {
        if (text == null) {
            return false;
        }
        final String normalized =
                text.trim()
                        .replace('_', ' ')
                        .replace('-', ' ')
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);
        return normalized.equals(NORMALIZED_RETRIGGER_PLAYING_LAUNCHER_CLIPS_LABEL)
                || normalized.contains(NORMALIZED_RETRIGGER_PLAYING_LAUNCHER_CLIPS_LABEL);
    }
}
