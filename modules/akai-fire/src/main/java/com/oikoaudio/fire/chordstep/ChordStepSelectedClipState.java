package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;

/**
 * Tracks the selected clip-slot state used by chord-step mode and centralizes the related
 * availability checks.
 */
public final class ChordStepSelectedClipState {
    private int slotIndex = -1;
    private boolean hasContent;
    private RgbLigthState color;

    public boolean refresh(final SelectedClipSlotState state) {
        final boolean changed = slotIndex != state.slotIndex() || hasContent != state.hasContent();
        slotIndex = state.slotIndex();
        hasContent = state.hasContent();
        color = state.color();
        return changed;
    }

    public NoteClipAvailability.Failure requireSelectedClipSlot(final boolean canHoldNoteData) {
        return NoteClipAvailability.requireSelectedClipSlot(canHoldNoteData, slotIndex >= 0);
    }

    public NoteClipAvailability.Failure requireClip(final boolean canHoldNoteData, final boolean hasLoadedContent) {
        final NoteClipAvailability.Failure slotFailure = requireSelectedClipSlot(canHoldNoteData);
        if (slotFailure != null) {
            return slotFailure;
        }
        return NoteClipAvailability.requireClipContent(hasContent || hasLoadedContent);
    }

    public int slotIndex() {
        return slotIndex;
    }

    public boolean hasContent() {
        return hasContent;
    }

    public RgbLigthState color() {
        return color;
    }
}
