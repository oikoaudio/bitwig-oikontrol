package com.oikoaudio.fire.nestedrhythm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedRhythmGeneratorTest {

    private final NestedRhythmGenerator generator = new NestedRhythmGenerator();

    @Test
    void generatorIsDeterministicForSettings() {
        final NestedRhythmGenerator.Settings settings = new NestedRhythmGenerator.Settings(
                60, 0.25, 3, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 5, 3, 0.7, 2, 1);

        final NestedRhythmPattern a = generator.generate(settings);
        final NestedRhythmPattern b = generator.generate(settings);

        assertEquals(starts(a), starts(b));
        assertEquals(velocities(a), velocities(b));
    }

    @Test
    void backHalfTupletLeavesFrontHalfAnchorsInPlace() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 3, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 3, 0.6, 0, 0));

        assertEquals(List.of(0, 420), startsInRange(pattern, 0, 840));
        assertEquals(3, startsInRange(pattern, 840, NestedRhythmGenerator.FINE_STEPS_PER_BAR).size());
    }

    @Test
    void ratchetReplacesSingleBeatInsideTupletRegion() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 3, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 5, 3, 0.6, 0, 0));

        assertEquals(5, startsInRange(pattern, 1260, 1680).size());
        assertEquals(List.of(0, 420), startsInRange(pattern, 0, 840));
        assertEquals(2, startsInRange(pattern, 840, 1260).size());
    }

    @Test
    void velocitiesAlwaysRetainContour() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.4, 7, NestedRhythmGenerator.TupletCoverage.FULL_BAR, 5, 1, 0.0, 3, 0));

        final List<Integer> velocities = velocities(pattern);
        final int min = velocities.stream().mapToInt(Integer::intValue).min().orElseThrow();
        final int max = velocities.stream().mapToInt(Integer::intValue).max().orElseThrow();

        assertNotEquals(min, max);
        assertTrue(max - min >= 10);
    }

    private List<Integer> starts(final NestedRhythmPattern pattern) {
        return pattern.events().stream().map(NestedRhythmPattern.PulseEvent::fineStart).toList();
    }

    private List<Integer> velocities(final NestedRhythmPattern pattern) {
        return pattern.events().stream().map(NestedRhythmPattern.PulseEvent::velocity).toList();
    }

    private List<Integer> startsInRange(final NestedRhythmPattern pattern, final int startInclusive,
                                        final int endExclusive) {
        return pattern.events().stream()
                .filter(event -> event.fineStart() >= startInclusive && event.fineStart() < endExclusive)
                .map(NestedRhythmPattern.PulseEvent::fineStart)
                .toList();
    }
}
