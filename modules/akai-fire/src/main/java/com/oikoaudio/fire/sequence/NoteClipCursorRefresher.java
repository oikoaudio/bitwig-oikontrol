package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;

import java.util.function.IntSupplier;

/**
 * Shared refresh sequence for note-clip based modes.
 * Rebuilds the locally tracked selected-clip state, resolves which clip slot should be active, then
 * runs the mode-specific cursor reset actions.
 */
public final class NoteClipCursorRefresher {
    private NoteClipCursorRefresher() {
    }

    public static boolean refresh(final ClipLauncherSlotBank slotBank,
                                  final int viewControlSelectedSlotIndex,
                                  final Runnable refreshSelectedClipState,
                                  final IntSupplier selectedSlotIndexSupplier,
                                  final Runnable... cursorResetActions) {
        refreshSelectedClipState.run();
        final boolean resolved = ClipSlotSelectionResolver.resolve(
                slotBank, viewControlSelectedSlotIndex, selectedSlotIndexSupplier.getAsInt());
        for (final Runnable action : cursorResetActions) {
            action.run();
        }
        return resolved;
    }
}
