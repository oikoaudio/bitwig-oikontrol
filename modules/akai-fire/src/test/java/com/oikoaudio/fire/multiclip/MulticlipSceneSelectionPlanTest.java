package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MulticlipSceneSelectionPlanTest {
    @Test
    void createsOnlyMissingClipsOnEligibleLaneTracks() {
        assertEquals(
                List.of(0, 3),
                MulticlipSceneSelectionPlan.missingEligibleLanes(
                        new boolean[] {true, true, false, true},
                        new boolean[] {false, true, false, false}));
    }
}
