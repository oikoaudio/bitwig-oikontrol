package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.lights.RgbLigthState;

/**
 * Snapshot of the currently selected clip slot state used by Akai modes.
 * Captures the selected slot index plus the pieces of bookkeeping the modes care about:
 * whether the slot has content and the color to use for pad feedback when color tracking is enabled.
 */
public final class SelectedClipSlotState {
    private final int slotIndex;
    private final boolean hasContent;
    private final RgbLigthState color;

    private SelectedClipSlotState(final int slotIndex, final boolean hasContent, final RgbLigthState color) {
        this.slotIndex = slotIndex;
        this.hasContent = hasContent;
        this.color = color;
    }

    public static SelectedClipSlotState fromValues(final int slotIndex,
                                                   final boolean hasContent,
                                                   final RgbLigthState color) {
        return new SelectedClipSlotState(slotIndex, hasContent, color);
    }

    public static SelectedClipSlotState scan(final ClipLauncherSlotBank slotBank, final RgbLigthState defaultColor) {
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            if (slot.exists().get() && slot.isSelected().get()) {
                final RgbLigthState color = defaultColor == null ? null : ColorLookup.getColor(slot.color().get());
                return new SelectedClipSlotState(i, slot.hasContent().get(), color);
            }
        }
        return new SelectedClipSlotState(-1, false, defaultColor);
    }

    public int slotIndex() {
        return slotIndex;
    }

    public boolean hasSelection() {
        return slotIndex >= 0;
    }

    public boolean hasContent() {
        return hasContent;
    }

    public RgbLigthState color() {
        return color;
    }
}
