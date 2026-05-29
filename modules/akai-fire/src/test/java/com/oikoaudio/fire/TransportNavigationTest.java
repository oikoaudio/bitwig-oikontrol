package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransportNavigationTest {
    @Test
    void playWhileStoppedLaunchesFromPlayStart() {
        assertEquals(AkaiFireOikontrolExtension.PlayPressAction.LAUNCH_FROM_PLAY_START,
                AkaiFireOikontrolExtension.playPressAction(false));
    }

    @Test
    void playWhileRunningStops() {
        assertEquals(AkaiFireOikontrolExtension.PlayPressAction.STOP,
                AkaiFireOikontrolExtension.playPressAction(true));
    }

    @Test
    void playWhileStoppedAfterArrangementResetRetriggersPlayingLauncherClips() {
        assertEquals(AkaiFireOikontrolExtension.PlayPressAction.RETRIGGER_LAUNCHERS_FROM_START,
                AkaiFireOikontrolExtension.playPressAction(false, true));
    }

    @Test
    void playWhileRunningIgnoresArrangementResetLatchAndStops() {
        assertEquals(AkaiFireOikontrolExtension.PlayPressAction.STOP,
                AkaiFireOikontrolExtension.playPressAction(true, true));
    }

    @Test
    void stopWhileStoppedGoesToArrangementStart() {
        assertEquals(AkaiFireOikontrolExtension.StopPressAction.GO_ARRANGEMENT_START,
                AkaiFireOikontrolExtension.stopPressAction(false));
    }

    @Test
    void stopWhilePlayingOnlyStops() {
        assertEquals(AkaiFireOikontrolExtension.StopPressAction.STOP,
                AkaiFireOikontrolExtension.stopPressAction(true));
    }

    @Test
    void cueMarkerTurnFindsNextMarkerAfterReference() {
        assertEquals(1, AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                4.0,
                1,
                new boolean[] {true, true, true},
                new double[] {0.0, 8.0, 16.0},
                3));
    }

    @Test
    void cueMarkerTurnFindsPreviousMarkerBeforeReference() {
        assertEquals(0, AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                8.0,
                -1,
                new boolean[] {true, true, true},
                new double[] {0.0, 8.0, 16.0},
                3));
    }

    @Test
    void cueMarkerTurnSkipsMissingMarkersAndStopsAtEdges() {
        assertEquals(2, AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                0.0,
                1,
                new boolean[] {true, false, true},
                new double[] {0.0, 8.0, 16.0},
                3));
        assertEquals(-1, AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                16.0,
                1,
                new boolean[] {true, true, true},
                new double[] {0.0, 8.0, 16.0},
                3));
    }

    @Test
    void cueMarkerTurnCanUseExistenceWhenItemCountHasNotArrivedYet() {
        assertEquals(1, AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                0.0,
                1,
                new boolean[] {true, true},
                new double[] {0.0, 8.0},
                0));
    }

    @Test
    void lastTouchedFineStepMatchesPreviousDefaultStep() {
        assertEquals(0.04, AkaiFireOikontrolExtension.mainCursorParameterStep(false), 0.0000001);
        assertEquals(0.01, AkaiFireOikontrolExtension.mainCursorParameterStep(true), 0.0000001);
    }

    @Test
    void retriggerPlayingLauncherClipsActionResolvesLikelyActionId() {
        final Application application = mock(Application.class);
        final Action action = mock(Action.class);
        when(application.getAction("retrigger_playing_launcher_clips")).thenReturn(action);

        assertSame(action, AkaiFireOikontrolExtension.resolveRetriggerPlayingLauncherClipsAction(application));
    }

    @Test
    void retriggerPlayingLauncherClipsActionFallsBackToGuiLabel() {
        final Application application = mock(Application.class);
        final Action other = action("other_action", "Other", "Other");
        final Action target = action("internal_unknown", "Retrigger playing Launcher clips", "");
        when(application.getActions()).thenReturn(new Action[] {other, target});

        assertSame(target, AkaiFireOikontrolExtension.resolveRetriggerPlayingLauncherClipsAction(application));
    }

    @Test
    void retriggerPlayingLauncherClipsActionMissingReturnsNull() {
        final Application application = mock(Application.class);
        when(application.getActions()).thenReturn(new Action[0]);

        assertNull(AkaiFireOikontrolExtension.resolveRetriggerPlayingLauncherClipsAction(application));
    }

    private static Action action(final String id, final String name, final String menuItemText) {
        final Action action = mock(Action.class);
        when(action.getId()).thenReturn(id);
        when(action.getName()).thenReturn(name);
        when(action.getMenuItemText()).thenReturn(menuItemText);
        return action;
    }
}
