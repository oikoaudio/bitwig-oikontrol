package com.oikoaudio.fire;

final class GlobalSettingsOverlayLatch {
    private boolean latched;

    boolean toggleLatch(final boolean available) {
        if (!available) {
            return false;
        }
        latched = !latched;
        return true;
    }

    void close() {
        latched = false;
    }

    boolean shouldBeActive(final boolean momentaryComboHeld, final boolean popupBrowserActive) {
        return !popupBrowserActive && (latched || momentaryComboHeld);
    }

    boolean isLatched() {
        return latched;
    }
}
