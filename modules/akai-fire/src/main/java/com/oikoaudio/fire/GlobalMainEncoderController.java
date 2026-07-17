package com.oikoaudio.fire;

import com.oikoaudio.fire.lights.BiColorLightState;
import java.util.function.BooleanSupplier;

/** Owns the global main-encoder role and press/turn gesture state. */
public final class GlobalMainEncoderController {
    public interface RoleActions {
        void adjustTempo(int inc, boolean fine);

        void adjustShuffle(int inc, boolean fine);

        void adjustTrackSelection(int inc, boolean pageStep);

        void adjustPlaybackStart(int inc);
    }

    public interface GlobalChordActions {
        void consumePatternGesture();

        void adjustPlaybackStartByGrid(int inc);

        void adjustPlaybackStartFine(int inc);

        void jumpToCueMarker(int inc);

        void zoomTimelineHorizontally(int inc);

        void zoomTimelineVertically(int inc);
    }

    private final BooleanSupplier drumGridRoleAvailable;
    private final BooleanSupplier autoPinFirstDrumMachine;

    private String currentRole = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    private String alternateRole = FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;
    private boolean pressed;
    private boolean turnedWhilePressed;

    public GlobalMainEncoderController(
            final BooleanSupplier drumGridRoleAvailable,
            final BooleanSupplier autoPinFirstDrumMachine) {
        this.drumGridRoleAvailable = drumGridRoleAvailable;
        this.autoPinFirstDrumMachine = autoPinFirstDrumMachine;
    }

    public String currentRole() {
        return resolveRole(currentRole);
    }

    public void applyStartupPreference(final String preferenceValue) {
        final String startupState =
                FireControlPreferences.normalizeMainEncoderStartupState(preferenceValue);
        currentRole =
                FireControlPreferences.MAIN_ENCODER_STARTUP_LAST_TOUCHED.equals(startupState)
                        ? FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED
                        : FireControlPreferences.normalizeMainEncoderRole(alternateRole);
    }

    public String cycleRole() {
        final String effectiveRole = currentRole();
        final String cycleSource =
                FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(effectiveRole)
                        ? alternateRole
                        : effectiveRole;
        final String nextRole =
                FireControlPreferences.nextAlternateMainEncoderRole(
                        cycleSource, drumGridRoleAvailable.getAsBoolean());
        currentRole = nextRole;
        alternateRole = nextRole;
        return currentRole();
    }

    public String toggleRole() {
        final String effectiveRole = currentRole();
        final String effectiveAlternateRole = resolveRole(alternateRole);
        currentRole =
                FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(effectiveRole)
                        ? (FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(
                                        effectiveAlternateRole)
                                ? FireControlPreferences.MAIN_ENCODER_TRACK_SELECT
                                : effectiveAlternateRole)
                        : FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
        return currentRole();
    }

    public void setPressed(final boolean pressed) {
        this.pressed = pressed;
        if (pressed) {
            turnedWhilePressed = false;
        }
    }

    public boolean isPressed() {
        return pressed;
    }

    public void markTurned() {
        if (pressed) {
            turnedWhilePressed = true;
        }
    }

    public boolean wasTurnedWhilePressed() {
        return turnedWhilePressed;
    }

    public boolean routeRoleTurn(final int inc, final boolean fine, final RoleActions actions) {
        final String role = currentRole();
        if (FireControlPreferences.MAIN_ENCODER_TEMPO.equals(role)) {
            actions.adjustTempo(inc, fine);
            return true;
        }
        if (FireControlPreferences.MAIN_ENCODER_SHUFFLE.equals(role)) {
            actions.adjustShuffle(inc, fine);
            return true;
        }
        if (FireControlPreferences.MAIN_ENCODER_TRACK_SELECT.equals(role)) {
            actions.adjustTrackSelection(inc, pressed);
            return true;
        }
        if (FireControlPreferences.MAIN_ENCODER_PLAYBACK_START.equals(role)) {
            actions.adjustPlaybackStart(inc);
            return true;
        }
        return false;
    }

    public boolean handleGlobalChord(
            final int inc,
            final boolean popupBrowserActive,
            final boolean patternPressed,
            final boolean shiftHeld,
            final boolean altHeld,
            final GlobalChordActions actions) {
        final MainEncoderGlobalChord.Action action =
                MainEncoderGlobalChord.resolve(
                        inc, popupBrowserActive, patternPressed, shiftHeld, altHeld);
        switch (action) {
            case PLAYBACK_START_GRID -> actions.adjustPlaybackStartByGrid(inc);
            case PLAYBACK_START_FINE -> {
                actions.consumePatternGesture();
                actions.adjustPlaybackStartFine(inc);
            }
            case CUE_MARKER -> {
                actions.consumePatternGesture();
                actions.jumpToCueMarker(inc);
            }
            case TIMELINE_ZOOM_HORIZONTAL -> actions.zoomTimelineHorizontally(inc);
            case TIMELINE_ZOOM_VERTICAL -> actions.zoomTimelineVertically(inc);
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    private String resolveRole(final String role) {
        final String normalizedRole = FireControlPreferences.normalizeMainEncoderRole(role);
        final boolean drumGridAvailable = drumGridRoleAvailable.getAsBoolean();
        if (FireControlPreferences.MAIN_ENCODER_TRACK_SELECT.equals(normalizedRole)
                && drumGridAvailable
                && autoPinFirstDrumMachine.getAsBoolean()) {
            return FireControlPreferences.MAIN_ENCODER_DRUM_GRID;
        }
        if (FireControlPreferences.MAIN_ENCODER_DRUM_GRID.equals(normalizedRole)
                && !drumGridAvailable) {
            return FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;
        }
        return normalizedRole;
    }

    static int cueMarkerIndexAfterTurn(
            final double reference,
            final int inc,
            final boolean[] exists,
            final double[] positions,
            final int itemCount) {
        final int observedLimit = itemCount > 0 ? itemCount : exists.length;
        final int limit = Math.min(Math.min(exists.length, positions.length), observedLimit);
        if (inc == 0 || limit == 0) {
            return -1;
        }
        final double epsilon = 0.000001;
        if (inc > 0) {
            for (int index = 0; index < limit; index++) {
                if (exists[index] && positions[index] > reference + epsilon) {
                    return index;
                }
            }
            return -1;
        }
        for (int index = limit - 1; index >= 0; index--) {
            if (exists[index] && positions[index] < reference - epsilon) {
                return index;
            }
        }
        return -1;
    }

    static int remotePageIndexAfterTurn(
            final int currentPage, final int pageCount, final int direction) {
        if (pageCount <= 0) {
            return currentPage;
        }
        return Math.max(0, Math.min(pageCount - 1, currentPage + direction));
    }

    static String remotePageCountLabel(final int pageIndex, final int pageCount) {
        return pageCount > 1 ? (pageIndex + 1) + "/" + pageCount : "";
    }

    static BiColorLightState remotePageNavigationLightState(
            final int currentPage, final int pageCount, final int direction) {
        if (pageCount <= 1) {
            return BiColorLightState.OFF;
        }
        if (direction < 0) {
            return currentPage > 0 ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
        }
        if (direction > 0) {
            return currentPage < pageCount - 1
                    ? BiColorLightState.AMBER_HALF
                    : BiColorLightState.OFF;
        }
        return BiColorLightState.OFF;
    }
}
