package com.oikoaudio.fire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSettingsOverlayLatchTest {

    @Test
    void latchKeepsOverlayActiveAfterMomentaryComboIsReleased() {
        final GlobalSettingsOverlayLatch latch = new GlobalSettingsOverlayLatch();

        assertTrue(latch.toggleLatch(true));

        assertTrue(latch.shouldBeActive(false, false));
    }

    @Test
    void secondAvailableToggleClosesLatchedOverlay() {
        final GlobalSettingsOverlayLatch latch = new GlobalSettingsOverlayLatch();
        latch.toggleLatch(true);

        assertTrue(latch.toggleLatch(true));

        assertFalse(latch.shouldBeActive(false, false));
        assertFalse(latch.isLatched());
    }

    @Test
    void popupBrowserSuppressesOverlayEvenWhenLatched() {
        final GlobalSettingsOverlayLatch latch = new GlobalSettingsOverlayLatch();
        latch.toggleLatch(true);

        assertFalse(latch.shouldBeActive(true, true));
    }

    @Test
    void unavailableToggleDoesNotChangeLatch() {
        final GlobalSettingsOverlayLatch latch = new GlobalSettingsOverlayLatch();

        assertFalse(latch.toggleLatch(false));

        assertFalse(latch.shouldBeActive(false, false));
    }
}
