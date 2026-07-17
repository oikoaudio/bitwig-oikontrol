package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlobalMainEncoderControllerTest {
    private static final class RecordingRoleActions
            implements GlobalMainEncoderController.RoleActions {
        private String call = "";

        @Override
        public void adjustTempo(final int inc, final boolean fine) {
            call = "tempo:" + inc + ":" + fine;
        }

        @Override
        public void adjustShuffle(final int inc, final boolean fine) {
            call = "shuffle:" + inc + ":" + fine;
        }

        @Override
        public void adjustTrackSelection(final int inc, final boolean pageStep) {
            call = "track:" + inc + ":" + pageStep;
        }

        @Override
        public void adjustPlaybackStart(final int inc) {
            call = "playback:" + inc;
        }
    }

    @Test
    void appliesStartupPreferenceAndTogglesTheLastAlternateRole() {
        final GlobalMainEncoderController controller =
                new GlobalMainEncoderController(() -> false, () -> false);

        controller.applyStartupPreference(FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET);
        assertEquals(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT, controller.currentRole());

        assertEquals(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED, controller.toggleRole());
        assertEquals(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT, controller.toggleRole());
    }

    @Test
    void resolvesAutoPinnedDrumRoleAndSkipsItWhenUnavailable() {
        final GlobalMainEncoderController drumController =
                new GlobalMainEncoderController(() -> true, () -> true);
        drumController.applyStartupPreference(
                FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET);
        assertEquals(FireControlPreferences.MAIN_ENCODER_DRUM_GRID, drumController.currentRole());

        final GlobalMainEncoderController nonDrumController =
                new GlobalMainEncoderController(() -> false, () -> false);
        nonDrumController.applyStartupPreference(
                FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET);
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE, nonDrumController.cycleRole());
        assertEquals(FireControlPreferences.MAIN_ENCODER_TEMPO, nonDrumController.cycleRole());
    }

    @Test
    void ownsPressAndTurnConsumptionState() {
        final GlobalMainEncoderController controller =
                new GlobalMainEncoderController(() -> false, () -> false);

        controller.setPressed(true);
        assertTrue(controller.isPressed());
        assertFalse(controller.wasTurnedWhilePressed());

        controller.markTurned();
        assertTrue(controller.wasTurnedWhilePressed());

        controller.setPressed(false);
        assertFalse(controller.isPressed());
        assertTrue(controller.wasTurnedWhilePressed());

        controller.setPressed(true);
        assertFalse(controller.wasTurnedWhilePressed());
    }

    @Test
    void routesGlobalRoleTurnsAndUsesPressForTrackPageSteps() {
        final GlobalMainEncoderController controller =
                new GlobalMainEncoderController(() -> false, () -> false);
        final RecordingRoleActions actions = new RecordingRoleActions();
        controller.applyStartupPreference(FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET);
        controller.setPressed(true);

        assertTrue(controller.routeRoleTurn(2, false, actions));
        assertEquals("track:2:true", actions.call);

        controller.toggleRole();
        assertFalse(controller.routeRoleTurn(1, false, actions));
    }
}
