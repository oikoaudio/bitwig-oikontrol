package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.PinnableCursorDevice;

/** Owns the global SHIFT+ALT+SELECT device-deletion gesture and its release latch. */
final class GlobalDeviceDeletionController {
    private final Host host;
    private boolean gestureActive;

    GlobalDeviceDeletionController(final Host host) {
        this.host = host;
    }

    boolean handleMainEncoderPress(final boolean pressed) {
        if (!pressed) {
            if (!gestureActive) {
                return false;
            }
            gestureActive = false;
            return true;
        }
        if (!host.shiftHeld() || !host.altHeld()) {
            return false;
        }
        gestureActive = true;
        final PinnableCursorDevice selected = host.selectedDevice();
        if (selected == null || !selected.exists().get()) {
            host.notifyAction("Delete Device", "No Device");
            return true;
        }
        selected.deleteObject();
        host.notifyAction("Delete Device", "Deleted");
        return true;
    }

    interface Host {
        boolean shiftHeld();

        boolean altHeld();

        PinnableCursorDevice selectedDevice();

        void notifyAction(String title, String value);
    }
}
