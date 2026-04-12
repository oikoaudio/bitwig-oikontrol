package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;

/**
 * Wires clip-slot observers needed to keep Akai mode state in sync with the currently selected clip.
 * Callers choose whether color and playback state should also be observed based on their lighting needs.
 */
public final class SelectedClipSlotObserver {
    private SelectedClipSlotObserver() {
    }

    public static void observe(final ClipLauncherSlotBank slotBank,
                               final boolean includeColor,
                               final boolean includePlaybackState,
                               final Runnable refresh) {
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            slot.exists().markInterested();
            slot.hasContent().markInterested();
            slot.isSelected().markInterested();
            slot.exists().addValueObserver(ignored -> refresh.run());
            slot.hasContent().addValueObserver(ignored -> refresh.run());
            slot.isSelected().addValueObserver(ignored -> refresh.run());

            if (includeColor) {
                slot.color().markInterested();
                slot.color().addValueObserver((r, g, b) -> refresh.run());
            }

            if (includePlaybackState) {
                slot.isPlaying().markInterested();
                slot.isRecording().markInterested();
                slot.isPlaying().addValueObserver(ignored -> refresh.run());
                slot.isRecording().addValueObserver(ignored -> refresh.run());
            }
        }
        refresh.run();
    }
}
