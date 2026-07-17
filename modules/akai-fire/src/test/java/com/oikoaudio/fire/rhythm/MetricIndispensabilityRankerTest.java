package com.oikoaudio.fire.rhythm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricIndispensabilityRankerTest {

    @Test
    void ranksFourFourSixteenthPositionsByMetricStrength() {
        final Map<Integer, MetricIndispensabilityRanker.RankedPosition> ranked =
                MetricIndispensabilityRanker.rank(
                        List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), 16);

        assertEquals(0, ranked.get(0).rankOrder());
        assertTrue(ranked.get(0).normalizedScore() > ranked.get(8).normalizedScore());
        assertTrue(ranked.get(8).normalizedScore() > ranked.get(4).normalizedScore());
        assertTrue(ranked.get(4).normalizedScore() > ranked.get(2).normalizedScore());
        assertTrue(ranked.get(2).normalizedScore() > ranked.get(1).normalizedScore());
    }

    @Test
    void rankingIsIndependentOfInputOrderAndDuplicates() {
        final Map<Integer, MetricIndispensabilityRanker.RankedPosition> ordered =
                MetricIndispensabilityRanker.rank(List.of(0, 4, 8, 12), 16);
        final Map<Integer, MetricIndispensabilityRanker.RankedPosition> shuffled =
                MetricIndispensabilityRanker.rank(List.of(12, 4, 0, 8, 4), 16);

        assertEquals(ordered, shuffled);
    }
}
