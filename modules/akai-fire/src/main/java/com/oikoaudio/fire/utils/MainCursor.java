package com.oikoaudio.fire.utils;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;

/*
 * Creates the main cursor for a Controller Extension
 */
public class MainCursor {
    private final CursorTrack cursorTrack;
    private final CursorRemoteControlsPage cursorRemoteControlsPage;
    private final PinnableCursorDevice cursorDevice;

    public MainCursor(ControllerHost host, int numSends, int numScenes){
        cursorTrack = host.createCursorTrack(numSends, numScenes);
        cursorDevice = cursorTrack.createCursorDevice();
        cursorRemoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
    }

    public CursorTrack track() {
        return cursorTrack;
    }

    public PinnableCursorDevice device() {
        return cursorDevice;
    }

    public CursorRemoteControlsPage remoteControlsPage() {
        return cursorRemoteControlsPage;
    }
}