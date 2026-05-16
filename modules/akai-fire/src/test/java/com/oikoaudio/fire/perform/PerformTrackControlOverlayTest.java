package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

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
                PerformClipLauncherMode.patternSceneNavigationLightState(false, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(false, false));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true));
    }
}
