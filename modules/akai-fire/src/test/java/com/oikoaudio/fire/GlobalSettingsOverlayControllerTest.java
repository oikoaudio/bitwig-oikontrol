package com.oikoaudio.fire;

import com.oikoaudio.fire.TopLevelModeState.Mode;
import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSettingsOverlayControllerTest {
    @Test
    void advancesPagesInControllerOrder() {
        final GlobalSettingsOverlayController.State state = new GlobalSettingsOverlayController.State();

        assertEquals(EncoderMode.CHANNEL, state.page());
        assertEquals(EncoderMode.MIXER, state.advancePage());
        assertEquals(EncoderMode.USER_2, state.advancePage());
        assertEquals(EncoderMode.USER_1, state.advancePage());
        assertEquals(EncoderMode.CHANNEL, state.advancePage());
    }

    @Test
    void combinesMomentaryAndLatchedActivationWhileBrowserSuppressesBoth() {
        final GlobalSettingsOverlayController.State state = new GlobalSettingsOverlayController.State();

        assertTrue(state.shouldBeActive(true, false));
        assertTrue(state.toggleLatch(true));
        assertTrue(state.shouldBeActive(false, false));
        assertFalse(state.shouldBeActive(true, true));
        assertFalse(state.shouldBeActive(false, true));
    }

    @Test
    void plainModeDismissalConsumesOnlyAnAlreadyActiveTarget() {
        final GlobalSettingsOverlayController.State state = new GlobalSettingsOverlayController.State();
        state.setActive(true);

        assertFalse(state.dismissForModeButton(Mode.DRUM, Mode.NOTE_PLAY, false, false));
        assertFalse(state.isActive());
        assertFalse(state.isLatched());

        final GlobalSettingsOverlayController.State alreadyInTarget = new GlobalSettingsOverlayController.State();
        alreadyInTarget.setActive(true);
        assertTrue(alreadyInTarget.dismissForModeButton(Mode.DRUM, Mode.DRUM, false, false));
        assertFalse(alreadyInTarget.isActive());
    }

    @Test
    void modifiersPreventPlainModeDismissal() {
        final GlobalSettingsOverlayController.State state = new GlobalSettingsOverlayController.State();
        state.setActive(true);
        state.toggleLatch(true);

        assertFalse(state.dismissForModeButton(Mode.DRUM, Mode.DRUM, true, false));
        assertTrue(state.isActive());
        assertTrue(state.isLatched());
    }
}
