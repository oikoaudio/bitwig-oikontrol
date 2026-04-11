package com.oikoaudio.fire.sequence;

final class SeqClipCopySourceResolver {
    private SeqClipCopySourceResolver() {
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
