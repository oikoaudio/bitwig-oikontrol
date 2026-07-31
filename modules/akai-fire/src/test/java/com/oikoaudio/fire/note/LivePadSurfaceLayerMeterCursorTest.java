package com.oikoaudio.fire.note;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class LivePadSurfaceLayerMeterCursorTest {
    @Test
    void noteAndDrumPadModesUseDistinctMeterCursorIds() {
        assertNotEquals(
                LivePadSurfaceLayer.meterCursorId("NOTE_PLAY_MODE_LAYER"),
                LivePadSurfaceLayer.meterCursorId("DRUM_PAD_PLAY_MODE_LAYER"));
    }
}
