package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrowserTransportControlsTest {

    @Test
    void playCommitsPopupBrowserWhenBrowserIsActive() {
        assertEquals(
                AkaiFireOikontrolExtension.BrowserTransportAction.COMMIT,
                AkaiFireOikontrolExtension.browserTransportAction(true, NoteAssign.PLAY, true));
    }

    @Test
    void stopCancelsPopupBrowserWhenBrowserIsActive() {
        assertEquals(
                AkaiFireOikontrolExtension.BrowserTransportAction.CANCEL,
                AkaiFireOikontrolExtension.browserTransportAction(true, NoteAssign.STOP, true));
    }

    @Test
    void browserTransportRoutingIgnoresReleaseAndInactiveBrowser() {
        assertEquals(
                AkaiFireOikontrolExtension.BrowserTransportAction.NONE,
                AkaiFireOikontrolExtension.browserTransportAction(true, NoteAssign.PLAY, false));
        assertEquals(
                AkaiFireOikontrolExtension.BrowserTransportAction.NONE,
                AkaiFireOikontrolExtension.browserTransportAction(false, NoteAssign.PLAY, true));
        assertEquals(
                AkaiFireOikontrolExtension.BrowserTransportAction.NONE,
                AkaiFireOikontrolExtension.browserTransportAction(true, NoteAssign.REC, true));
    }
}
