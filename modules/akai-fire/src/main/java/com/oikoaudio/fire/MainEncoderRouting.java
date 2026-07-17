package com.oikoaudio.fire;

import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.control.TouchEncoder;

/** Concrete access to the global routing and gesture policy shared by mode-local main encoders. */
public final class MainEncoderRouting {
    public enum Role {
        LAST_TOUCHED,
        NOTE_REPEAT,
        TEMPO,
        SHUFFLE,
        TRACK_SELECT,
        PLAYBACK_START,
        DRUM_GRID
    }

    private final AkaiFireOikontrolExtension extension;

    public MainEncoderRouting(final AkaiFireOikontrolExtension extension) {
        this.extension = extension;
    }

    public TouchEncoder encoder() {
        return extension.getMainEncoder();
    }

    public boolean isPopupBrowserActive() {
        return extension.isPopupBrowserActive();
    }

    public void routeBrowserTurn(final int increment) {
        extension.routeBrowserMainEncoder(increment);
    }

    public void routeBrowserPress(final boolean pressed) {
        extension.routeBrowserMainEncoderPress(pressed);
    }

    public void markTurned() {
        extension.markMainEncoderTurned();
    }

    public boolean routeGlobalChord(final int increment) {
        return extension.handleMainEncoderGlobalChord(increment);
    }

    public boolean routeRoleTurn(final int increment, final boolean fine) {
        return extension.routeGlobalMainEncoderRole(increment, fine);
    }

    public void setPressed(final boolean pressed) {
        extension.setMainEncoderPressed(pressed);
    }

    public boolean wasTurnedWhilePressed() {
        return extension.wasMainEncoderTurnedWhilePressed();
    }

    public boolean isShiftHeld() {
        return extension.isGlobalShiftHeld();
    }

    public boolean isAltHeld() {
        return extension.isGlobalAltHeld();
    }

    public Role currentRole() {
        return switch (extension.getMainEncoderRolePreference()) {
            case AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE -> Role.NOTE_REPEAT;
            case AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE -> Role.TEMPO;
            case AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE -> Role.SHUFFLE;
            case AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE -> Role.TRACK_SELECT;
            case AkaiFireOikontrolExtension.MAIN_ENCODER_PLAYBACK_START_ROLE -> Role.PLAYBACK_START;
            case AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE -> Role.DRUM_GRID;
            default -> Role.LAST_TOUCHED;
        };
    }

    public void cycleRole() {
        extension.cycleMainEncoderRolePreference();
    }

    public void toggleRole() {
        extension.toggleMainEncoderRolePreference();
    }

    public void toggleCurrentDeviceWindow() {
        extension.toggleCurrentDeviceWindow();
    }

    public void adjustCursorParameter(final int increment, final boolean fine) {
        extension.adjustMainCursorParameter(increment, fine);
    }

    public void showCursorParameterInfo() {
        extension.showMainCursorParameterInfo();
    }

    public void showTempoInfo() {
        extension.showTempoInfo();
    }

    public void showShuffleInfo() {
        extension.showGrooveShuffleInfo();
    }

    public void showSelectedTrackInfo() {
        extension.showSelectedTrackInfo(false);
    }

    public boolean handleReset(
            final boolean resettable,
            final String fallbackLabel,
            final String unavailableDetail,
            final Runnable resetAction,
            final Runnable showAction) {
        return extension.handleKnobModeEncoderReset(
                true, resettable, fallbackLabel, unavailableDetail, resetAction, showAction);
    }

    public ParameterEncoderBinding.ExplicitResetControl resetControl() {
        return extension.knobModeEncoderResetControl();
    }
}
