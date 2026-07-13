package com.oikoaudio.fire.perform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerformPageStateTest {
    @Test
    void launcherTransitionsToEachExclusiveTopLevelPageAndBack() {
        final PerformPageState launcher = PerformPageState.launcher();

        assertEquals(PerformPageState.Page.MIX, launcher.withTrackActionMode(true).page());
        assertEquals(PerformPageState.Page.SCENE_LAUNCH, launcher.toggleSceneLaunch().page());
        assertEquals(PerformPageState.Page.BIRDS_EYE, launcher.toggleBirdsEye().page());
        assertEquals(launcher, launcher.toggleSceneLaunch().toggleSceneLaunch());
        assertEquals(launcher, launcher.toggleBirdsEye().toggleBirdsEye());
    }

    @Test
    void enteringAnyTopLevelPageLeavesMixDeviceOverlays() {
        final PerformPageState deviceLayers =
                PerformPageState.launcher()
                        .withTrackActionMode(true)
                        .withMixDeviceMode(true)
                        .withDeviceLayers(true);

        assertEquals(PerformPageState.Page.SCENE_LAUNCH, deviceLayers.toggleSceneLaunch().page());
        assertEquals(PerformPageState.Page.BIRDS_EYE, deviceLayers.toggleBirdsEye().page());
        assertEquals(
                PerformPageState.Page.LAUNCHER, deviceLayers.withTrackActionMode(false).page());
    }

    @Test
    void mixDeviceAndDeviceLayersAreExplicitMixSubpages() {
        final PerformPageState mix = PerformPageState.launcher().withTrackActionMode(true);
        final PerformPageState devices = mix.withMixDeviceMode(true).withMixDevicePage(2);
        final PerformPageState layers = devices.withDeviceLayers(true);

        assertTrue(devices.isTrackActionMode());
        assertTrue(devices.isMixDeviceMode());
        assertEquals(2, devices.mixDevicePageIndex());
        assertEquals(PerformPageState.Page.DEVICE_LAYERS, layers.page());
        assertTrue(layers.isDeviceLayers());

        final PerformPageState backToMix = layers.leaveMixDeviceMode();
        assertEquals(PerformPageState.Page.MIX, backToMix.page());
        assertEquals(0, backToMix.mixDevicePageIndex());
    }

    @Test
    void reapplyingRememberedMixModePreservesItsCurrentSubpage() {
        final PerformPageState devices =
                PerformPageState.launcher()
                        .withTrackActionMode(true)
                        .withMixDeviceMode(true)
                        .withMixDevicePage(1);

        assertEquals(devices, devices.withTrackActionMode(true));
    }

    @Test
    void applyingRememberedLauncherModeDoesNotDiscardSceneOrBirdsEye() {
        final PerformPageState scene = PerformPageState.launcher().toggleSceneLaunch();
        final PerformPageState birdsEye = PerformPageState.launcher().toggleBirdsEye();

        assertEquals(scene, scene.withTrackActionMode(false));
        assertEquals(birdsEye, birdsEye.withTrackActionMode(false));
        assertEquals(PerformPageState.Page.MIX, scene.toggleTrackActionMode().page());
        assertEquals(PerformPageState.Page.MIX, birdsEye.toggleTrackActionMode().page());
    }

    @Test
    void deviceLayerOverlayReturnsToTheSameDevicePage() {
        final PerformPageState devices =
                PerformPageState.launcher()
                        .withTrackActionMode(true)
                        .withMixDeviceMode(true)
                        .withMixDevicePage(2);

        assertEquals(devices, devices.withDeviceLayers(true).withDeviceLayers(false));
    }

    @Test
    void leavingBirdsEyeOnlyChangesBirdsEyeState() {
        final PerformPageState scene = PerformPageState.launcher().toggleSceneLaunch();
        assertEquals(scene, scene.leaveBirdsEye());

        final PerformPageState launcher =
                PerformPageState.launcher().toggleBirdsEye().leaveBirdsEye();
        assertEquals(PerformPageState.Page.LAUNCHER, launcher.page());
        assertFalse(launcher.isBirdsEye());
    }
}
