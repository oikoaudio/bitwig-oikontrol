package com.oikoaudio.fire.multiclip;

import com.oikoaudio.fire.lights.RgbLightState;

/** Drum XOX-compatible selected, playing, and queued light states. */
final class MulticlipPlaybackLight {
    private MulticlipPlaybackLight() {}

    static RgbLightState render(
            final RgbLightState color,
            final boolean selected,
            final boolean playing,
            final boolean queued,
            final RgbLightState idle,
            final int blinkTick) {
        if (playing) {
            return blinkTick % 8 < 4
                    ? selected ? color.getBrightest() : color
                    : selected ? color : color.getDimmed();
        }
        if (queued) {
            return blinkTick % 2 == 0 ? selected ? color.getBrightest() : color : color.getDimmed();
        }
        return idle;
    }
}
