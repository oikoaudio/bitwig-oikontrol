package com.oikoaudio.fire.sequence;

/**
 * Shared availability checks for note-clip based modes.
 * Centralizes the user-facing failure states used when a mode needs a note-capable track, a selected
 * clip slot, or an existing clip with content.
 */
public final class NoteClipAvailability {
    private static final Failure AUDIO_TRACK = new Failure("Audio Track", "Use note track", "Use note track");
    private static final Failure NO_CLIP_SELECTED = new Failure("No Clip", "Select clip", "Select clip");
    private static final Failure NO_CLIP_CONTENT =
            new Failure("No Clip", "Create or Select Clip", "Create or select clip");

    private NoteClipAvailability() {
    }

    public static Failure requireSelectedClipSlot(final boolean canHoldNoteData, final boolean hasSelectedClipSlot) {
        if (!canHoldNoteData) {
            return AUDIO_TRACK;
        }
        if (!hasSelectedClipSlot) {
            return NO_CLIP_SELECTED;
        }
        return null;
    }

    public static Failure requireClipContent(final boolean hasContent) {
        return hasContent ? null : NO_CLIP_CONTENT;
    }

    public record Failure(String title, String oledDetail, String popupDetail) {
    }
}
