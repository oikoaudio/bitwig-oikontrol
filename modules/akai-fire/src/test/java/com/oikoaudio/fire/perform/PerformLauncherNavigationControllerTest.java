package com.oikoaudio.fire.perform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerformLauncherNavigationControllerTest {
    @Test
    void routesEachPageFromTheSharedPageState() {
        assertEquals(
                PerformLauncherNavigationController.Route.LAUNCHER,
                PerformLauncherNavigationController.route(PerformPageState.launcher()));
        assertEquals(
                PerformLauncherNavigationController.Route.SCENE,
                PerformLauncherNavigationController.route(
                        PerformPageState.launcher().toggleSceneLaunch()));
        assertEquals(
                PerformLauncherNavigationController.Route.BIRDS_EYE,
                PerformLauncherNavigationController.route(
                        PerformPageState.launcher().toggleBirdsEye()));
        assertEquals(
                PerformLauncherNavigationController.Route.MIX,
                PerformLauncherNavigationController.route(
                        PerformPageState.launcher().withTrackActionMode(true)));
    }

    @Test
    void slotActionsPreserveModifierAndRecordingPrecedence() {
        assertEquals(
                PerformLauncherNavigationController.SlotAction.RECORD,
                PerformLauncherNavigationController.slotAction(
                        input(true, true, true, true, true, true, true, true)));
        assertEquals(
                PerformLauncherNavigationController.SlotAction.DELETE,
                PerformLauncherNavigationController.slotAction(
                        input(true, true, true, false, true, true, true, true)));
        assertEquals(
                PerformLauncherNavigationController.SlotAction.COPY,
                PerformLauncherNavigationController.slotAction(
                        input(true, true, true, false, false, true, true, true)));
        assertEquals(
                PerformLauncherNavigationController.SlotAction.SELECT,
                PerformLauncherNavigationController.slotAction(
                        input(true, true, true, false, false, false, true, true)));
        assertEquals(
                PerformLauncherNavigationController.SlotAction.STOP_RECORDING,
                PerformLauncherNavigationController.slotAction(
                        input(true, true, true, false, false, false, false, true)));
        assertEquals(
                PerformLauncherNavigationController.SlotAction.LAUNCH,
                PerformLauncherNavigationController.slotAction(
                        input(true, true, true, false, false, false, false, false)));
        assertEquals(
                PerformLauncherNavigationController.SlotAction.CREATE,
                PerformLauncherNavigationController.slotAction(
                        input(true, true, false, false, false, false, false, false)));
    }

    @Test
    void sceneActionsAndNavigationClampAtBounds() {
        assertEquals(
                PerformLauncherNavigationController.SceneAction.DELETE,
                PerformLauncherNavigationController.sceneAction(true, true, true, true, true));
        assertEquals(
                PerformLauncherNavigationController.SceneAction.COPY,
                PerformLauncherNavigationController.sceneAction(true, true, false, true, true));
        assertEquals(
                PerformLauncherNavigationController.SceneAction.SELECT,
                PerformLauncherNavigationController.sceneAction(true, true, false, false, true));
        assertEquals(
                PerformLauncherNavigationController.SceneAction.LAUNCH,
                PerformLauncherNavigationController.sceneAction(true, true, false, false, false));
        assertEquals(8, PerformLauncherNavigationController.nextOffset(4, 12, 4, 1));
        assertEquals(8, PerformLauncherNavigationController.nextOffset(8, 12, 4, 1));
        assertEquals(0, PerformLauncherNavigationController.nextOffset(2, 12, 4, -1));
    }

    @Test
    void birdsEyeJumpUsesExplicitTrackAndSceneOffsets() {
        assertEquals(
                new PerformLauncherNavigationController.BirdsEyeJump(true, 16, 0),
                PerformLauncherNavigationController.birdsEyeJump(
                        1, PerformLayout.vertical(), 64, 20));
        assertEquals(
                new PerformLauncherNavigationController.BirdsEyeJump(false, 0, 0),
                PerformLauncherNavigationController.birdsEyeJump(
                        -1, PerformLayout.vertical(), 64, 20));
    }

    private static PerformLauncherNavigationController.SlotInput input(
            final boolean trackExists,
            final boolean slotExists,
            final boolean hasContent,
            final boolean record,
            final boolean delete,
            final boolean copy,
            final boolean select,
            final boolean recording) {
        return new PerformLauncherNavigationController.SlotInput(
                true,
                trackExists,
                slotExists,
                false,
                record,
                delete,
                copy,
                select,
                recording,
                hasContent);
    }
}
