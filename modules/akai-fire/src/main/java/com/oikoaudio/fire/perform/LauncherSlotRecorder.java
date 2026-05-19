package com.oikoaudio.fire.perform;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;

final class LauncherSlotRecorder {
    private LauncherSlotRecorder() {
    }

    static int firstFreeSlotIndex(final ClipLauncherSlotBank slotBank) {
        for (int index = 0; index < slotBank.getSizeOfBank(); index++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(index);
            if (slot.exists().get()
                    && !slot.hasContent().get()
                    && !slot.isRecording().get()
                    && !slot.isRecordingQueued().get()) {
                return index;
            }
        }
        return -1;
    }

    static boolean hasLauncherActivity(final ClipLauncherSlotBank slotBank) {
        for (int index = 0; index < slotBank.getSizeOfBank(); index++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(index);
            if (slot.exists().get()
                    && (slot.isPlaying().get()
                    || slot.isPlaybackQueued().get()
                    || slot.isRecording().get()
                    || slot.isRecordingQueued().get())) {
                return true;
            }
        }
        return false;
    }
}
