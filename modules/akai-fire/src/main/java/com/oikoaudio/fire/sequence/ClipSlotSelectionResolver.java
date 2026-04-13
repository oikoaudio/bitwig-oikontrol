package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;

/**
 * Resolves which clip slot selection should drive a mode.
 * Reuses the clip slot currently selected by view control when possible, otherwise falls back to a
 * currently selected slot already reflected in local mode state, then selects a playing or recording
 * slot as a last resort.
 */
public final class ClipSlotSelectionResolver {
    private ClipSlotSelectionResolver() {
    }

    public static boolean resolve(final ClipLauncherSlotBank slotBank,
                                  final int preferredSlotIndex,
                                  final int selectedSlotIndex) {
        if (isSelectedSlot(slotBank, preferredSlotIndex)) {
            return true;
        }
        if (selectedSlotIndex >= 0) {
            return true;
        }
        return selectPlayingSlot(slotBank);
    }

    static boolean selectPlayingSlot(final ClipLauncherSlotBank slotBank) {
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            if (slot.exists().get() && (slot.isPlaying().get() || slot.isRecording().get())) {
                slot.select();
                return true;
            }
        }
        return false;
    }

    private static boolean isSelectedSlot(final ClipLauncherSlotBank slotBank, final int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slotBank.getSizeOfBank()) {
            return false;
        }
        final ClipLauncherSlot slot = slotBank.getItemAt(slotIndex);
        return slot.exists().get() && slot.isSelected().get();
    }
}
