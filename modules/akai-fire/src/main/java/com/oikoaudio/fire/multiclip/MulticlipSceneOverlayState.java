package com.oikoaudio.fire.multiclip;

/** Latches ALT-first scene overlay entry for the duration of one ALT hold. */
public final class MulticlipSceneOverlayState {
    private boolean altHeld;
    private boolean active;

    public void altPressed(final boolean hasHeldSteps) {
        altHeld = true;
        active = !hasHeldSteps;
    }

    public void heldStepsChanged(final boolean hasHeldSteps) {
        // Entry is decided on ALT-down; releasing a pre-held step must not switch roles mid-hold.
    }

    public void altReleased() {
        altHeld = false;
        active = false;
    }

    public boolean isActive() {
        return altHeld && active;
    }
}
