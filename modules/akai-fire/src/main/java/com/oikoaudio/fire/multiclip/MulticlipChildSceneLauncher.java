package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ClipLauncherSlot;

final class MulticlipChildSceneLauncher {
    private MulticlipChildSceneLauncher() {}

    static int launch(final Iterable<ClipLauncherSlot> childSlots) {
        int launched = 0;
        for (final ClipLauncherSlot slot : childSlots) {
            slot.launch();
            launched++;
        }
        return launched;
    }
}
