package com.oikoaudio.fire.multiclip;

/** A first-step write waiting for Bitwig to finish creating its launcher clip. */
public record PendingStepCreation(MulticlipTargetIdentity target, int row, int step, int velocity) {
    public boolean matches(final MulticlipTargetIdentity currentTarget) {
        return target.equals(currentTarget);
    }
}
