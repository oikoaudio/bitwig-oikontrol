package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;

/**
 * Resolves which clip slot selection should drive a mode. Reuses the clip slot currently selected
 * by view control when possible, otherwise falls back to a currently selected slot already
 * reflected in local mode state. Passive refresh never changes the DAW's clip selection.
 */
public final class ClipSlotSelectionResolver {
    private ClipSlotSelectionResolver() {}

    public static boolean resolve(
            final ClipLauncherSlotBank slotBank,
            final int preferredSlotIndex,
            final int selectedSlotIndex) {
        if (isSelectedSlot(slotBank, preferredSlotIndex)) {
            return true;
        }
        if (selectedSlotIndex >= 0) {
            return true;
        }
        return false;
    }

    private static boolean isSelectedSlot(
            final ClipLauncherSlotBank slotBank, final int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slotBank.getSizeOfBank()) {
            return false;
        }
        final ClipLauncherSlot slot = slotBank.getItemAt(slotIndex);
        return slot.exists().get() && slot.isSelected().get();
    }
}
