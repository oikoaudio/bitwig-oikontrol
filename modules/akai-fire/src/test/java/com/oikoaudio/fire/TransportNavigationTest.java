package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransportNavigationTest {
    @Test
    void playWhileStoppedLaunchesFromPlayStart() {
        assertEquals(
                AkaiFireOikontrolExtension.PlayPressAction.LAUNCH_FROM_PLAY_START,
                AkaiFireOikontrolExtension.playPressAction(false));
    }

    @Test
    void playWhileRunningStops() {
        assertEquals(
                AkaiFireOikontrolExtension.PlayPressAction.STOP,
                AkaiFireOikontrolExtension.playPressAction(true));
    }

    @Test
    void playWhileStoppedAfterArrangementResetRetriggersPlayingLauncherClips() {
        assertEquals(
                AkaiFireOikontrolExtension.PlayPressAction.RETRIGGER_LAUNCHERS_FROM_START,
                AkaiFireOikontrolExtension.playPressAction(false, true));
    }

    @Test
    void playWhileRunningIgnoresArrangementResetLatchAndStops() {
        assertEquals(
                AkaiFireOikontrolExtension.PlayPressAction.STOP,
                AkaiFireOikontrolExtension.playPressAction(true, true));
    }

    @Test
    void stopWhileStoppedGoesToArrangementStart() {
        assertEquals(
                AkaiFireOikontrolExtension.StopPressAction.GO_ARRANGEMENT_START,
                AkaiFireOikontrolExtension.stopPressAction(false));
    }

    @Test
    void stopWhilePlayingOnlyStops() {
        assertEquals(
                AkaiFireOikontrolExtension.StopPressAction.STOP,
                AkaiFireOikontrolExtension.stopPressAction(true));
    }

    @Test
    void cueMarkerTurnFindsNextMarkerAfterReference() {
        assertEquals(
                1,
                AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                        4.0,
                        1,
                        new boolean[] {true, true, true},
                        new double[] {0.0, 8.0, 16.0},
                        3));
    }

    @Test
    void cueMarkerTurnFindsPreviousMarkerBeforeReference() {
        assertEquals(
                0,
                AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                        8.0,
                        -1,
                        new boolean[] {true, true, true},
                        new double[] {0.0, 8.0, 16.0},
                        3));
    }

    @Test
    void cueMarkerTurnSkipsMissingMarkersAndStopsAtEdges() {
        assertEquals(
                2,
                AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                        0.0,
                        1,
                        new boolean[] {true, false, true},
                        new double[] {0.0, 8.0, 16.0},
                        3));
        assertEquals(
                -1,
                AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                        16.0,
                        1,
                        new boolean[] {true, true, true},
                        new double[] {0.0, 8.0, 16.0},
                        3));
    }

    @Test
    void cueMarkerTurnCanUseExistenceWhenItemCountHasNotArrivedYet() {
        assertEquals(
                1,
                AkaiFireOikontrolExtension.cueMarkerIndexAfterTurn(
                        0.0, 1, new boolean[] {true, true}, new double[] {0.0, 8.0}, 0));
    }

    @Test
    void lastTouchedFineStepMatchesPreviousDefaultStep() {
        assertEquals(0.04, AkaiFireOikontrolExtension.mainCursorParameterStep(false), 0.0000001);
        assertEquals(0.01, AkaiFireOikontrolExtension.mainCursorParameterStep(true), 0.0000001);
    }

    @Test
    void akaiFireRequiresBitwigSixApi() {
        assertEquals(25, new AkaiFireOikontrolDefinition().getRequiredAPIVersion());
    }

    @Test
    void plainPatternReleaseTogglesAutomationWrite() {
        assertEquals(
                AkaiFireOikontrolExtension.PatternReleaseAction.TOGGLE_AUTOMATION_WRITE,
                AkaiFireOikontrolExtension.patternReleaseAction(false, false, false));
    }

    @Test
    void patternReleaseChordsKeepTheirActions() {
        assertEquals(
                AkaiFireOikontrolExtension.PatternReleaseAction.TOGGLE_METRONOME,
                AkaiFireOikontrolExtension.patternReleaseAction(false, true, false));
        assertEquals(
                AkaiFireOikontrolExtension.PatternReleaseAction.TOGGLE_LAUNCHER_OVERDUB,
                AkaiFireOikontrolExtension.patternReleaseAction(false, false, true));
        assertEquals(
                AkaiFireOikontrolExtension.PatternReleaseAction.NONE,
                AkaiFireOikontrolExtension.patternReleaseAction(true, false, false));
    }

    @Test
    void automationWriteNextStateTreatsArrangerAndLauncherAsUnified() {
        assertEquals(true, AkaiFireOikontrolExtension.automationWriteNextState(false, false));
        assertEquals(false, AkaiFireOikontrolExtension.automationWriteNextState(true, false));
        assertEquals(false, AkaiFireOikontrolExtension.automationWriteNextState(false, true));
        assertEquals(false, AkaiFireOikontrolExtension.automationWriteNextState(true, true));
    }

    @Test
    void altRecordTogglesArrangerOverdub() {
        assertEquals(true, AkaiFireOikontrolExtension.overdubNextState(false));
        assertEquals(false, AkaiFireOikontrolExtension.overdubNextState(true));
    }

    @Test
    void launcherOverdubActivationArmsTouchAutomationWrite() {
        final AkaiFireOikontrolExtension.LauncherOverdubPressPlan plan =
                AkaiFireOikontrolExtension.launcherOverdubPressPlan(false, false, false);

        assertEquals(true, plan.launcherOverdubEnabled());
        assertEquals(true, plan.automationWriteEnabled());
        assertEquals(true, plan.touchAutomationWriteMode());
        assertEquals(true, plan.automationWriteAutoEnabled());
    }

    @Test
    void launcherOverdubDoesNotClaimExistingAutomationWrite() {
        final AkaiFireOikontrolExtension.LauncherOverdubPressPlan plan =
                AkaiFireOikontrolExtension.launcherOverdubPressPlan(false, true, false);

        assertEquals(true, plan.launcherOverdubEnabled());
        assertEquals(true, plan.automationWriteEnabled());
        assertEquals(true, plan.touchAutomationWriteMode());
        assertEquals(false, plan.automationWriteAutoEnabled());
    }

    @Test
    void launcherOverdubDeactivationOnlyClearsAutomationWriteItAutoEnabled() {
        final AkaiFireOikontrolExtension.LauncherOverdubPressPlan autoEnabledPlan =
                AkaiFireOikontrolExtension.launcherOverdubPressPlan(true, true, true);
        final AkaiFireOikontrolExtension.LauncherOverdubPressPlan manuallyEnabledPlan =
                AkaiFireOikontrolExtension.launcherOverdubPressPlan(true, true, false);

        assertEquals(false, autoEnabledPlan.launcherOverdubEnabled());
        assertEquals(false, autoEnabledPlan.automationWriteEnabled());
        assertEquals(false, autoEnabledPlan.touchAutomationWriteMode());
        assertEquals(false, autoEnabledPlan.automationWriteAutoEnabled());

        assertEquals(false, manuallyEnabledPlan.launcherOverdubEnabled());
        assertEquals(true, manuallyEnabledPlan.automationWriteEnabled());
        assertEquals(false, manuallyEnabledPlan.touchAutomationWriteMode());
        assertEquals(false, manuallyEnabledPlan.automationWriteAutoEnabled());
    }
}
