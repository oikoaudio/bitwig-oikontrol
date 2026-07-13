package com.oikoaudio.fire.fugue;

/** Tracks whether Fugue may replace the selected clip's derived MIDI lines. */
public final class FugueLineState {
    private static final int LINE_COUNT = 4;

    private final boolean[] enabled = new boolean[LINE_COUNT];
    private boolean protectedDerivedMaterial;

    public FugueLineState() {
        enabled[FugueClipAdapter.SOURCE_CHANNEL] = true;
    }

    public void enterClip(final boolean hasDerivedNotes) {
        enabled[FugueClipAdapter.SOURCE_CHANNEL] = true;
        for (int line = FugueClipAdapter.FIRST_DERIVED_CHANNEL;
                line <= FugueClipAdapter.LAST_DERIVED_CHANNEL;
                line++) {
            enabled[line] = false;
        }
        protectedDerivedMaterial = hasDerivedNotes;
    }

    public boolean isEnabled(final int line) {
        return enabled[line];
    }

    public boolean isProtected() {
        return protectedDerivedMaterial;
    }

    /** Returns whether the line was toggled. Protected material requires an explicit claim. */
    public boolean toggle(final int line) {
        if (line < FugueClipAdapter.SOURCE_CHANNEL
                || line > FugueClipAdapter.LAST_DERIVED_CHANNEL
                || protectedDerivedMaterial && line >= FugueClipAdapter.FIRST_DERIVED_CHANNEL) {
            return false;
        }
        enabled[line] = !enabled[line];
        return true;
    }

    public void claimAllDerivedLines() {
        protectedDerivedMaterial = false;
        for (int line = FugueClipAdapter.FIRST_DERIVED_CHANNEL;
                line <= FugueClipAdapter.LAST_DERIVED_CHANNEL;
                line++) {
            enabled[line] = true;
        }
    }
}
