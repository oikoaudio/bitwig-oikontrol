package com.oikoaudio.fire.sequence;

/**
 * Chooses which clip slot should act as the source for clip-row copy actions.
 * Prefers the locally selected slot, then falls back to a playing slot, then a recording slot.
 */
final class ClipRowCopySourceResolver {
    private ClipRowCopySourceResolver() {
    }

    static int resolve(final int selectedSlotIndex, final boolean[] playingSlots, final boolean[] recordingSlots) {
        if (selectedSlotIndex >= 0) {
            return selectedSlotIndex;
        }
        for (int i = 0; i < playingSlots.length; i++) {
            if (playingSlots[i]) {
                return i;
            }
        }
        for (int i = 0; i < recordingSlots.length; i++) {
            if (recordingSlots[i]) {
                return i;
            }
        }
        return -1;
    }
}
