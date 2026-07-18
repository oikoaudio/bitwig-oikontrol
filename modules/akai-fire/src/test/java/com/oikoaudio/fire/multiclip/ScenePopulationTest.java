package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScenePopulationTest {
    @ParameterizedTest
    @CsvSource({"false,0,4,NEW", "true,0,4,EMPTY", "true,1,4,PARTIAL", "true,4,4,POPULATED"})
    void classifiesMappedLaneClipPopulation(
            final boolean exists,
            final int clips,
            final int lanes,
            final ScenePopulation expected) {
        assertEquals(expected, ScenePopulation.of(exists, clips, lanes));
    }
}
