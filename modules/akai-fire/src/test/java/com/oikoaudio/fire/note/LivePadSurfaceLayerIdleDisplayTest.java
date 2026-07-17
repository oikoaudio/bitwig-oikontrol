package com.oikoaudio.fire.note;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

class LivePadSurfaceLayerIdleDisplayTest {
    @Test
    void liveMetersOnlyShowWhileTransportIsPlaying() {
        assertEquals(
                true,
                LivePadSurfaceLayer.shouldShowLiveMeters(
                        true, false, false, EncoderMode.MIXER, true));
        assertEquals(
                false,
                LivePadSurfaceLayer.shouldShowLiveMeters(
                        true, false, false, EncoderMode.MIXER, false));
        assertEquals(
                false,
                LivePadSurfaceLayer.shouldShowLiveMeters(
                        true, true, false, EncoderMode.MIXER, true));
        assertEquals(
                false,
                LivePadSurfaceLayer.shouldShowLiveMeters(
                        true, false, true, EncoderMode.MIXER, true));
        assertEquals(
                false,
                LivePadSurfaceLayer.shouldShowLiveMeters(
                        true, false, false, EncoderMode.CHANNEL, true));
    }

    @Test
    void contextIdleRefreshWaitsForTransientToExpire() {
        assertEquals(false, LivePadSurfaceLayer.shouldRefreshLiveContextIdle(false, true, true));
        assertEquals(false, LivePadSurfaceLayer.shouldRefreshLiveContextIdle(true, false, true));
        assertEquals(false, LivePadSurfaceLayer.shouldRefreshLiveContextIdle(false, false, false));
        assertEquals(true, LivePadSurfaceLayer.shouldRefreshLiveContextIdle(false, false, true));
    }

    @Test
    void noteChordDisplayStaysVisibleDuringShortNoteGaps() {
        assertEquals(true, LivePadSurfaceLayer.shouldHoldLiveNoteChordDisplay(true, 1_000, 1_500));
        assertEquals(false, LivePadSurfaceLayer.shouldHoldLiveNoteChordDisplay(true, 1_500, 1_500));
        assertEquals(
                false, LivePadSurfaceLayer.shouldHoldLiveNoteChordDisplay(false, 1_000, 1_500));
    }

    @Test
    void estimatesStepInputTotalStepsFromClipLength() {
        assertEquals(16, LivePadSurfaceLayer.estimatedStepInputTotalSteps(4.0, 0.25));
        assertEquals(1, LivePadSurfaceLayer.estimatedStepInputTotalSteps(0.25, 0.25));
        assertEquals(-1, LivePadSurfaceLayer.estimatedStepInputTotalSteps(0.0, 0.25));
        assertEquals(-1, LivePadSurfaceLayer.estimatedStepInputTotalSteps(4.0, 0.0));
    }

    @Test
    void holdCapturedPadLightBlinksBetweenBrightAndBase() {
        final RgbLightState base = new RgbLightState(0, 72, 110, true);

        assertEquals(
                base.getBrightest(), LivePadSurfaceLayer.heldLivePadLight(base, true, true, 0));
        assertEquals(base, LivePadSurfaceLayer.heldLivePadLight(base, true, true, 4));
        assertEquals(
                base.getBrightest(), LivePadSurfaceLayer.heldLivePadLight(base, true, false, 4));
        assertEquals(base, LivePadSurfaceLayer.heldLivePadLight(base, false, false, 0));
    }
}
