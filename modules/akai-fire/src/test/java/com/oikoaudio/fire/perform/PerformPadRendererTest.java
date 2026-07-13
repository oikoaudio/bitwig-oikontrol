package com.oikoaudio.fire.perform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.RgbLightState;
import org.junit.jupiter.api.Test;

class PerformPadRendererTest {
    private static final RgbLightState COLOR = new RgbLightState(10, 90, 30, true);

    @Test
    void launcherSlotsCoverMissingEmptyQueuedPlayingAndRecordingStates() {
        assertEquals(
                RgbLightState.OFF,
                PerformPadRenderer.slot(PerformPadRenderer.SlotSnapshot.missing(), 0));
        assertEquals(
                RgbLightState.GRAY_2,
                PerformPadRenderer.slot(
                        new PerformPadRenderer.SlotSnapshot(
                                true, false, true, false, false, false, false, false, COLOR),
                        0));
        assertEquals(
                COLOR.getBrightest(),
                PerformPadRenderer.slot(
                        new PerformPadRenderer.SlotSnapshot(
                                true, true, false, true, false, false, false, false, COLOR),
                        0));
        assertEquals(
                COLOR.getDimmed(),
                PerformPadRenderer.slot(
                        new PerformPadRenderer.SlotSnapshot(
                                true, true, false, false, true, false, false, false, COLOR),
                        1));
        assertEquals(
                COLOR.getBrightend(),
                PerformPadRenderer.slot(
                        new PerformPadRenderer.SlotSnapshot(
                                true, true, false, false, false, true, false, false, COLOR),
                        0));
        assertEquals(
                COLOR.getBrightest(),
                PerformPadRenderer.slot(
                        new PerformPadRenderer.SlotSnapshot(
                                true, true, true, false, false, false, false, true, COLOR),
                        0));
    }

    @Test
    void sceneLaunchCoversUnavailablePendingSelectedRecordingAndPlayingStates() {
        assertEquals(
                RgbLightState.OFF,
                PerformPadRenderer.scene(
                        new PerformPadRenderer.SceneSnapshot(
                                false, COLOR, false, false, false, false),
                        0));
        assertEquals(
                COLOR.getBrightest(),
                PerformPadRenderer.scene(
                        new PerformPadRenderer.SceneSnapshot(
                                true, COLOR, true, false, false, false),
                        0));
        assertEquals(
                COLOR.getBrightest(),
                PerformPadRenderer.scene(
                        new PerformPadRenderer.SceneSnapshot(
                                true, COLOR, false, true, false, false),
                        0));
        assertEquals(
                COLOR,
                PerformPadRenderer.scene(
                        new PerformPadRenderer.SceneSnapshot(
                                true, COLOR, false, false, true, false),
                        1));
        assertEquals(
                COLOR.getDimmed(),
                PerformPadRenderer.scene(
                        new PerformPadRenderer.SceneSnapshot(
                                true, COLOR, false, false, false, true),
                        4));
    }

    @Test
    void mixRowsCoverSelectionQueueSoloMuteArmAndMissingTracks() {
        assertEquals(
                RgbLightState.OFF,
                PerformPadRenderer.trackAction(
                        PerformPadRenderer.TrackSnapshot.missing(
                                PerformPadRenderer.TrackAction.SELECT),
                        0));
        assertEquals(
                COLOR.getBrightest(),
                PerformPadRenderer.trackAction(
                        new PerformPadRenderer.TrackSnapshot(
                                PerformPadRenderer.TrackAction.SELECT,
                                true,
                                COLOR,
                                true,
                                false,
                                false,
                                false,
                                false,
                                false),
                        0));
        assertEquals(
                COLOR.getDimmed(),
                PerformPadRenderer.trackAction(
                        new PerformPadRenderer.TrackSnapshot(
                                PerformPadRenderer.TrackAction.SELECT,
                                true,
                                COLOR,
                                false,
                                true,
                                true,
                                false,
                                false,
                                false),
                        1));
        assertEquals(
                PerformPadRenderer.TrackAction.SOLO.color().getBrightest(),
                PerformPadRenderer.trackAction(
                        new PerformPadRenderer.TrackSnapshot(
                                PerformPadRenderer.TrackAction.SOLO,
                                true,
                                COLOR,
                                false,
                                false,
                                false,
                                true,
                                false,
                                false),
                        0));
        assertEquals(
                PerformPadRenderer.TrackAction.MUTE.color().getDimmed(),
                PerformPadRenderer.trackAction(
                        new PerformPadRenderer.TrackSnapshot(
                                PerformPadRenderer.TrackAction.MUTE,
                                true,
                                COLOR,
                                false,
                                false,
                                false,
                                false,
                                false,
                                false),
                        0));
        assertEquals(
                PerformPadRenderer.TrackAction.ARM.color(),
                PerformPadRenderer.trackAction(
                        new PerformPadRenderer.TrackSnapshot(
                                PerformPadRenderer.TrackAction.ARM,
                                true,
                                COLOR,
                                false,
                                false,
                                false,
                                false,
                                false,
                                true),
                        0));
    }

    @Test
    void deviceLayerAndBirdsEyePagesUsePureSnapshots() {
        assertEquals(
                RgbLightState.OFF,
                PerformPadRenderer.mixDevice(
                        new PerformPadRenderer.MixDeviceSnapshot(false, COLOR, true, true)));
        assertEquals(
                COLOR.getSoftDimmed(),
                PerformPadRenderer.mixDevice(
                        new PerformPadRenderer.MixDeviceSnapshot(true, COLOR, false, true)));
        assertEquals(
                RgbLightState.OFF,
                PerformPadRenderer.deviceLayer(
                        new PerformPadRenderer.DeviceLayerSnapshot(
                                PerformPadRenderer.TrackAction.SELECT,
                                false,
                                COLOR,
                                false,
                                false,
                                false)));
        assertEquals(
                COLOR.getSoftDimmed(),
                PerformPadRenderer.deviceLayer(
                        new PerformPadRenderer.DeviceLayerSnapshot(
                                PerformPadRenderer.TrackAction.SELECT,
                                true,
                                COLOR,
                                false,
                                false,
                                false)));
        assertEquals(new RgbLightState(0, 36, 84, true), PerformPadRenderer.birdsEye(true, false));
        assertEquals(new RgbLightState(0, 108, 127, true), PerformPadRenderer.birdsEye(true, true));
    }

    @Test
    void settingsLogoHasExplicitPadBounds() {
        assertEquals(new RgbLightState(127, 20, 0, true), PerformPadRenderer.settingsLogo(0));
        assertEquals(RgbLightState.OFF, PerformPadRenderer.settingsLogo(-1));
        assertEquals(RgbLightState.OFF, PerformPadRenderer.settingsLogo(64));
    }
}
