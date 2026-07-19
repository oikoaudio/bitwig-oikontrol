package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;

final class MulticlipChildClipCreator {
    private MulticlipChildClipCreator() {}

    static void create(
            final ClipLauncherSlotBank childSlots,
            final int visibleScene,
            final int lengthInBeats) {
        childSlots.createEmptyClip(visibleScene, lengthInBeats);
    }
}
