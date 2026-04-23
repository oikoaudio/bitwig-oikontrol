package com.oikoaudio.fire.perform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformRemoteControlMappingTest {

    @Test
    void altSelectsSecondFourRemoteParameters() {
        assertEquals(0, PerformClipLauncherMode.remoteParameterIndex(0, false));
        assertEquals(3, PerformClipLauncherMode.remoteParameterIndex(3, false));
        assertEquals(4, PerformClipLauncherMode.remoteParameterIndex(0, true));
        assertEquals(7, PerformClipLauncherMode.remoteParameterIndex(3, true));
    }

    @Test
    void remotePageTurnsClampToAvailablePages() {
        assertEquals(1, PerformClipLauncherMode.remotePageIndexAfterTurn(0, 4, 1));
        assertEquals(3, PerformClipLauncherMode.remotePageIndexAfterTurn(2, 4, 8));
        assertEquals(0, PerformClipLauncherMode.remotePageIndexAfterTurn(2, 4, -8));
        assertEquals(2, PerformClipLauncherMode.remotePageIndexAfterTurn(2, 0, 1));
    }
}
