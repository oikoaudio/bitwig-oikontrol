package com.oikoaudio.fire;

final class MainEncoderGlobalChord {
    enum Action {
        NONE,
        PLAYBACK_START_GRID,
        PLAYBACK_START_FINE,
        CUE_MARKER,
        TIMELINE_ZOOM_HORIZONTAL,
        TIMELINE_ZOOM_VERTICAL
    }

    private MainEncoderGlobalChord() {}

    static Action resolve(
            final int increment,
            final boolean popupBrowserActive,
            final boolean patternHeld,
            final boolean shiftHeld,
            final boolean altHeld) {
        if (increment == 0 || popupBrowserActive) {
            return Action.NONE;
        }
        if (patternHeld) {
            return shiftHeld ? Action.PLAYBACK_START_FINE : Action.CUE_MARKER;
        }
        if (altHeld) {
            return shiftHeld ? Action.TIMELINE_ZOOM_VERTICAL : Action.TIMELINE_ZOOM_HORIZONTAL;
        }
        if (shiftHeld) {
            return Action.PLAYBACK_START_GRID;
        }
        return Action.NONE;
    }
}
