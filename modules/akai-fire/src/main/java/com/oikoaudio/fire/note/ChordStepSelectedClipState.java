package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;

/**
 * Tracks the selected clip-slot state used by chord-step mode and centralizes the related
 * availability checks.
 */
final class ChordStepSelectedClipState {
    private int slotIndex = -1;
    private boolean hasContent;
    private RgbLigthState color;

    boolean refresh(final SelectedClipSlotState state) {
        final boolean changed = slotIndex != state.slotIndex() || hasContent != state.hasContent();
        slotIndex = state.slotIndex();
        hasContent = state.hasContent();
        color = state.color();
        return changed;
    }

    NoteClipAvailability.Failure requireSelectedClipSlot(final boolean canHoldNoteData) {
        return NoteClipAvailability.requireSelectedClipSlot(canHoldNoteData, slotIndex >= 0);
    }

    NoteClipAvailability.Failure requireClip(final boolean canHoldNoteData, final boolean hasLoadedContent) {
        final NoteClipAvailability.Failure slotFailure = requireSelectedClipSlot(canHoldNoteData);
        if (slotFailure != null) {
            return slotFailure;
        }
        return NoteClipAvailability.requireClipContent(hasContent || hasLoadedContent);
    }

    int slotIndex() {
        return slotIndex;
    }

    boolean hasContent() {
        return hasContent;
    }

    RgbLigthState color() {
        return color;
    }
}
