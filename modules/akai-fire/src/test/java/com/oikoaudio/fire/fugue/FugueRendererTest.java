package com.oikoaudio.fire.fugue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.melodic.MelodicPattern;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FugueRendererTest {
    @Test
    void mapsLongPatternsIntoSixteenBuckets() {
        final List<MelodicPattern.Step> steps = new ArrayList<>(FuguePattern.emptySteps());
        steps.set(5, new MelodicPattern.Step(5, true, false, 60, 96, 1, 1, false, false, 0, 0));
        final FuguePattern pattern = new FuguePattern(steps, 32);

        assertEquals(60, FugueRenderer.bucketStep(pattern, 2, 32).pitch());
        assertEquals(2, FugueRenderer.bucketStep(pattern, 2, 32).index());
        assertEquals(10, FugueRenderer.bucketStart(5, 32));
        assertEquals(12, FugueRenderer.bucketEnd(5, 32));
    }

    @Test
    void mapsPlaybackAndShortLoopsToVisibleBuckets() {
        assertEquals(8, FugueRenderer.playingBucket(16, 32));
        assertEquals(-1, FugueRenderer.playingBucket(32, 32));
        assertEquals(8, FugueRenderer.activeBucketCount(8));
        assertEquals(16, FugueRenderer.activeBucketCount(64));
    }

    @Test
    void rendersLineEnableAndStatusStates() {
        assertEquals(BiColorLightState.GREEN_FULL, FugueRenderer.lineLight(true));
        assertEquals(BiColorLightState.GREEN_HALF, FugueRenderer.lineLight(false));
        assertEquals(BiColorLightState.AMBER_FULL, FugueRenderer.lineStatusLight(true));
        assertEquals(BiColorLightState.GREEN_HALF, FugueRenderer.lineStatusLight(false));
    }
}
