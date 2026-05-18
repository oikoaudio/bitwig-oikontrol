package com.oikoaudio.fire;

final class MainEncoderGlobalChord {
    enum Action {
        NONE,
        PLAYBACK_START_GRID,
        PLAY_POSITION_GRID,
        PLAY_POSITION_FINE,
        TIMELINE_ZOOM_HORIZONTAL,
        TIMELINE_ZOOM_VERTICAL
    }

    private MainEncoderGlobalChord() {
    }

    static Action resolve(final int increment,
                          final boolean popupBrowserActive,
                          final boolean patternHeld,
                          final boolean shiftHeld,
                          final boolean altHeld) {
        if (increment == 0 || popupBrowserActive) {
            return Action.NONE;
        }
        if (patternHeld) {
            return shiftHeld ? Action.PLAY_POSITION_FINE : Action.PLAY_POSITION_GRID;
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
