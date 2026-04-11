package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccentButtonModelTest {

    @Test
    void togglesAndReportsMatchingLightAndLabel() {
        final AccentButtonModel model = new AccentButtonModel();

        assertEquals("Off", model.label());
        assertEquals(BiColorLightState.AMBER_HALF, model.lightState());
        assertEquals(AccentLatchState.Transition.PRESSED, model.handlePressed(true));
        assertTrue(model.isHolding());

        assertEquals(AccentLatchState.Transition.TOGGLED_ON_RELEASE, model.handlePressed(false));
        assertFalse(model.isHolding());
        assertTrue(model.isActive());
        assertEquals("On", model.label());
        assertEquals(BiColorLightState.AMBER_FULL, model.lightState());
    }

    @Test
    void heldModificationSuppressesReleaseToggle() {
        final AccentButtonModel model = new AccentButtonModel();

        model.handlePressed(true);
        model.markModified();

        assertEquals(AccentLatchState.Transition.MODIFIED_RELEASE, model.handlePressed(false));
        assertFalse(model.isActive());
        assertEquals("Off", model.label());
        assertEquals(BiColorLightState.AMBER_HALF, model.lightState());
    }

    @Test
    void latchedEditsDoNotBlockLaterToggleOff() {
        final AccentButtonModel model = new AccentButtonModel();

        model.handlePressed(true);
        model.handlePressed(false);
        assertTrue(model.isActive());

        model.markModified();

        model.handlePressed(true);
        assertEquals(AccentLatchState.Transition.TOGGLED_ON_RELEASE, model.handlePressed(false));
        assertFalse(model.isActive());
        assertEquals("Off", model.label());
    }

    @Test
    void currentVelocityTracksLatchState() {
        final AccentButtonModel model = new AccentButtonModel();

        assertEquals(96, model.currentVelocity(96));
        model.handlePressed(true);
        model.handlePressed(false);
        assertEquals(model.accentedVelocity(), model.currentVelocity(96));
    }
}
