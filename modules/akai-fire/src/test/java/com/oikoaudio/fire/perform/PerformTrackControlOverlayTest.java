package com.oikoaudio.fire.perform;

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
}
