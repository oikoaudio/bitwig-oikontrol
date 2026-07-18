package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScenePopulationTest {
    @ParameterizedTest
    @CsvSource({"0,4,NEW", "1,4,PARTIAL", "4,4,POPULATED"})
    void classifiesOnlyMappedChildLaneClips(
            final int clips, final int lanes, final ScenePopulation expected) {
        assertEquals(expected, ScenePopulation.ofChildClips(clips, lanes));
    }
}
