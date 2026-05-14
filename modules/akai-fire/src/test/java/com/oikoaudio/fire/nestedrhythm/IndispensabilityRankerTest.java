package com.oikoaudio.fire.nestedrhythm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IndispensabilityRankerTest {

    @Test
    void ranksFourFourQuarterNotesByMetricIndispensability() {
        final Map<Integer, RankedPulse> ranked = IndispensabilityRanker.rank(
                List.of(0, 420, 840, 1260), NestedRhythmGenerator.FINE_STEPS_PER_WHOLE);

        assertTrue(ranked.get(0).normalizedScore() > ranked.get(840).normalizedScore());
        assertTrue(ranked.get(840).normalizedScore() > ranked.get(1260).normalizedScore());
        assertTrue(ranked.get(1260).normalizedScore() > ranked.get(420).normalizedScore());
    }

    @Test
    void ranksSixEightDottedQuarterAnchorAboveEighthInteriors() {
        final Map<Integer, RankedPulse> ranked = IndispensabilityRanker.rank(
                List.of(0, 210, 420, 630, 840, 1050), NestedRhythmGenerator.fineStepsPerBar(6, 8));

        assertTrue(ranked.get(0).normalizedScore() > ranked.get(630).normalizedScore());
        assertTrue(ranked.get(630).normalizedScore() > ranked.get(210).normalizedScore());
        assertTrue(ranked.get(630).normalizedScore() > ranked.get(840).normalizedScore());
    }

    @Test
    void ranksSixFourMidpointAboveInteriorQuarterNotes() {
        final Map<Integer, RankedPulse> ranked = IndispensabilityRanker.rank(
                List.of(0, 420, 840, 1260, 1680, 2100), NestedRhythmGenerator.fineStepsPerBar(6, 4));

        assertTrue(ranked.get(0).normalizedScore() > ranked.get(1260).normalizedScore());
        assertTrue(ranked.get(1260).normalizedScore() > ranked.get(420).normalizedScore());
        assertTrue(ranked.get(1260).normalizedScore() > ranked.get(2100).normalizedScore());
    }

    @Test
    void ranksMultiBarPhraseDownbeatAboveLaterBarStarts() {
        final Map<Integer, RankedPulse> ranked = IndispensabilityRanker.rank(
                List.of(0, 420, 840, 1260, 1680, 2100, 2520, 2940),
                NestedRhythmGenerator.fineStepsPerBar(4, 4) * 2);

        assertTrue(ranked.get(0).normalizedScore() > ranked.get(1680).normalizedScore());
        assertTrue(ranked.get(1680).normalizedScore() > ranked.get(840).normalizedScore());
        assertTrue(ranked.get(2520).normalizedScore() > ranked.get(420).normalizedScore());
    }
}
