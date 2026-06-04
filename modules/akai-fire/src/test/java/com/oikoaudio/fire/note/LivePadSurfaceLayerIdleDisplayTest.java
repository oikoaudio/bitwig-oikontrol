package com.oikoaudio.fire.note;

import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LivePadSurfaceLayerIdleDisplayTest {
    @Test
    void liveMetersOnlyShowWhileTransportIsPlaying() {
        assertEquals(true, LivePadSurfaceLayer.shouldShowLiveMeters(
                true, false, false, EncoderMode.CHANNEL, true));
        assertEquals(false, LivePadSurfaceLayer.shouldShowLiveMeters(
                true, false, false, EncoderMode.CHANNEL, false));
        assertEquals(false, LivePadSurfaceLayer.shouldShowLiveMeters(
                true, true, false, EncoderMode.CHANNEL, true));
        assertEquals(false, LivePadSurfaceLayer.shouldShowLiveMeters(
                true, false, true, EncoderMode.MIXER, true));
    }
}
