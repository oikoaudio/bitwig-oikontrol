package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformTrackControlOverlayTest {

    @Test
    void topRowPadsSelectTracksByDefault() {
        assertEquals("Select", PerformClipLauncherMode.trackControlActionForPad(0, false));
        assertEquals("Select", PerformClipLauncherMode.trackControlActionForPad(15, false));
    }

    @Test
    void altTopRowPadsStopTracks() {
        assertEquals("Stop", PerformClipLauncherMode.trackControlActionForPad(0, true));
        assertEquals("Stop", PerformClipLauncherMode.trackControlActionForPad(15, true));
    }

    @Test
    void lowerTrackControlRowsKeepMixerActions() {
        assertEquals("Solo", PerformClipLauncherMode.trackControlActionForPad(16, false));
        assertEquals("Mute", PerformClipLauncherMode.trackControlActionForPad(32, false));
        assertEquals("Arm", PerformClipLauncherMode.trackControlActionForPad(48, false));
    }

    @Test
    void selectRowUsesTrackColorForAvailableTracks() {
        final RgbLigthState trackColor = new RgbLigthState(10, 90, 30, true);

        assertEquals(trackColor, PerformClipLauncherMode.mixSelectPadColor(trackColor, false, false));
        assertEquals(trackColor.getDimmed(), PerformClipLauncherMode.mixSelectPadColor(trackColor, false, true));
        assertEquals(trackColor.getBrightest(), PerformClipLauncherMode.mixSelectPadColor(trackColor, true, true));
    }

    @Test
    void mixStatusLedsFollowSoloAndMuteProjectState() {
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.mixStatusLightState(false, true, true, 1));
        assertEquals(BiColorLightState.AMBER_FULL,
                PerformClipLauncherMode.mixStatusLightState(true, true, false, 1));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.mixStatusLightState(true, false, true, 1));
        assertEquals(BiColorLightState.AMBER_FULL,
                PerformClipLauncherMode.mixStatusLightState(true, false, true, 2));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.mixStatusLightState(true, true, false, 2));
    }

    @Test
    void patternSceneNavigationIsUnlitInMix() {
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(false, false, 0, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(false, false, 0, false, 0, 0, 1, false));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, false, 0, false, 0, 0, -1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, false, 0, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, false, 0, 0, -1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 1, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 1, false, 0, 0, -1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 0, 3, -1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 0, 3, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 2, 3, -1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 2, 3, 1, true));
    }

    @Test
    void mixDevicePadsMapRowsToPagedTrackDevices() {
        assertEquals(0, PerformClipLauncherMode.mixDeviceIndexForPad(0, 0));
        assertEquals(0, PerformClipLauncherMode.mixDeviceIndexForPad(15, 0));
        assertEquals(1, PerformClipLauncherMode.mixDeviceIndexForPad(16, 0));
        assertEquals(2, PerformClipLauncherMode.mixDeviceIndexForPad(32, 0));
        assertEquals(3, PerformClipLauncherMode.mixDeviceIndexForPad(48, 0));
        assertEquals(4, PerformClipLauncherMode.mixDeviceIndexForPad(0, 1));
        assertEquals(5, PerformClipLauncherMode.mixDeviceIndexForPad(16, 1));
        assertEquals(6, PerformClipLauncherMode.mixDeviceIndexForPad(32, 1));
        assertEquals(7, PerformClipLauncherMode.mixDeviceIndexForPad(48, 1));
    }

    @Test
    void mixDeviceRowsMapToPagedTrackDevices() {
        assertEquals(0, PerformClipLauncherMode.mixDeviceIndexForRow(0, 0));
        assertEquals(3, PerformClipLauncherMode.mixDeviceIndexForRow(3, 0));
        assertEquals(4, PerformClipLauncherMode.mixDeviceIndexForRow(0, 1));
        assertEquals(7, PerformClipLauncherMode.mixDeviceIndexForRow(3, 1));
        assertEquals(-1, PerformClipLauncherMode.mixDeviceIndexForRow(4, 1));
    }

    @Test
    void mixDevicePadColorShowsEnabledState() {
        final RgbLigthState trackColor = new RgbLigthState(10, 90, 30, true);

        assertEquals(trackColor.getBrightest(), PerformClipLauncherMode.mixDevicePadColor(trackColor, true, true));
        assertEquals(trackColor, PerformClipLauncherMode.mixDevicePadColor(trackColor, true, false));
        assertEquals(trackColor.getDimmed(), PerformClipLauncherMode.mixDevicePadColor(trackColor, false, false));
        assertEquals(trackColor.getSoftDimmed(), PerformClipLauncherMode.mixDevicePadColor(trackColor, false, true));
    }

    @Test
    void mixDevicePadActionTitlesDistinguishSelectFromAltToggle() {
        assertEquals("Device Select", PerformClipLauncherMode.mixDeviceActionTitle(false, true));
        assertEquals("Device On", PerformClipLauncherMode.mixDeviceActionTitle(true, true));
        assertEquals("Device Off", PerformClipLauncherMode.mixDeviceActionTitle(true, false));
    }

    @Test
    void rowWideDeviceToggleTurnsOffWhenAnyVisibleDeviceIsEnabled() {
        assertEquals(false, PerformClipLauncherMode.rowWideDeviceToggleTarget(true));
        assertEquals(true, PerformClipLauncherMode.rowWideDeviceToggleTarget(false));
        assertEquals("Device Row Off", PerformClipLauncherMode.rowWideDeviceToggleTitle(false));
        assertEquals("Device Row On", PerformClipLauncherMode.rowWideDeviceToggleTitle(true));
    }

    @Test
    void deviceSelectionMemoryStoresValidDeviceSlotsByAbsoluteTrack() {
        final Map<Integer, Integer> memory = new HashMap<>();

        PerformClipLauncherMode.rememberMixDeviceSelection(memory, 19, 5);

        assertEquals(5, PerformClipLauncherMode.rememberedMixDeviceSelection(memory, 19));
        assertEquals(-1, PerformClipLauncherMode.rememberedMixDeviceSelection(memory, 18));
    }

    @Test
    void deviceSelectionMemoryIgnoresInvalidAddresses() {
        final Map<Integer, Integer> memory = new HashMap<>();

        PerformClipLauncherMode.rememberMixDeviceSelection(memory, -1, 2);
        PerformClipLauncherMode.rememberMixDeviceSelection(memory, 3, -1);
        PerformClipLauncherMode.rememberMixDeviceSelection(memory, 3, 8);

        assertEquals(-1, PerformClipLauncherMode.rememberedMixDeviceSelection(memory, -1));
        assertEquals(-1, PerformClipLauncherMode.rememberedMixDeviceSelection(memory, 3));
    }

}
