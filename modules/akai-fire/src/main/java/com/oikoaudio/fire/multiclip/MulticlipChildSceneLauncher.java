package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ClipLauncherSlot;

final class MulticlipChildSceneLauncher {
    private static final String DEFAULT_QUANTIZATION = "default";
    private static final String FROM_START = "from_start";

    private MulticlipChildSceneLauncher() {}

    static int launch(final Iterable<ClipLauncherSlot> childSlots) {
        int launched = 0;
        for (final ClipLauncherSlot slot : childSlots) {
            slot.launchWithOptions(DEFAULT_QUANTIZATION, FROM_START);
            launched++;
        }
        return launched;
    }
}
