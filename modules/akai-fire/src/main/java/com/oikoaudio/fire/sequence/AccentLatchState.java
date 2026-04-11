package com.oikoaudio.fire.sequence;

public final class AccentLatchState {
    public enum Transition {
        PRESSED,
        TOGGLED_ON_RELEASE,
        MODIFIED_RELEASE
    }

    private boolean active;
    private boolean held;
    private boolean modified;

    public boolean isActive() {
        return active;
    }

    public boolean isHeld() {
        return held;
    }

    public void markModified() {
        modified = true;
    }

    public void clearHeld() {
        held = false;
    }

    public Transition handlePressed(final boolean pressed) {
        if (pressed) {
            held = true;
            return Transition.PRESSED;
        }
        held = false;
        final boolean toggled = !modified;
        if (toggled) {
            active = !active;
        }
        modified = false;
        return toggled ? Transition.TOGGLED_ON_RELEASE : Transition.MODIFIED_RELEASE;
    }
}
