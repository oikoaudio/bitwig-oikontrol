package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MulticlipSceneOverlayStateTest {
    @Test
    void altFirstEntersMomentaryOverlay() {
        final MulticlipSceneOverlayState state = new MulticlipSceneOverlayState();

        state.altPressed(false);
        assertTrue(state.isActive());

        state.altReleased();
        assertFalse(state.isActive());
    }

    @Test
    void heldStepFirstKeepsLaneEditingForTheEntireAltHold() {
        final MulticlipSceneOverlayState state = new MulticlipSceneOverlayState();

        state.altPressed(true);
        assertFalse(state.isActive());

        state.heldStepsChanged(false);
        assertFalse(state.isActive());
    }
}
