package com.oikoaudio.fire.nestedrhythm;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedRhythmGeneratorTest {

    private final NestedRhythmGenerator generator = new NestedRhythmGenerator();

    @Test
    void generatorIsDeterministicForSettings() {
        final NestedRhythmGenerator.Settings settings = new NestedRhythmGenerator.Settings(
                60, 0.75, 5, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 4, 2, 1, 0.7, 100, 2, 1);

        final NestedRhythmPattern a = generator.generate(settings);
        final NestedRhythmPattern b = generator.generate(settings);

        assertEquals(starts(a), starts(b));
        assertEquals(velocities(a), velocities(b));
    }

    @Test
    void defaultsProduceQuarterAnchorsOnly() {
        final NestedRhythmPattern pattern = generator.generate(defaultSettings());

        assertEquals(List.of(0, 420, 840, 1260), starts(pattern));
    }

    @Test
    void densityMinimumKeepsAnchorsAndRequiredSubdivisionLeads() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 7, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 4, 2, 0, 0.6, 100, 0, 0));

        assertEquals(List.of(0, 420, 840), starts(pattern));
    }

    @Test
    void backHalfTupletLeavesFrontHalfAnchorsInPlace() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 0, 1, 0, 0.6, 100, 0, 0));

        assertEquals(List.of(0, 420), startsInRange(pattern, 0, 840));
        assertEquals(List.of(840, 1120, 1400),
                startsInRange(pattern, 840, NestedRhythmGenerator.FINE_STEPS_PER_BAR));
    }

    @Test
    void tupletPhaseMovesHalfBarTupletToFrontHalf() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 1, 0, 1, 0, 0.6, 100, 0, 0));

        assertEquals(List.of(0, 280, 560), startsInRange(pattern, 0, 840));
        assertEquals(List.of(840, 1260), startsInRange(pattern, 840, NestedRhythmGenerator.FINE_STEPS_PER_BAR));
    }

    @Test
    void ratchetOverridesTupletInsideClaimedBeatSpan() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 4, 1, 2, 0.6, 100, 0, 0));

        assertEquals(List.of(0), startsInRange(pattern, 0, 420));
        assertEquals(List.of(420, 525, 630, 735), startsInRange(pattern, 420, 840));
        assertEquals(List.of(840, 1120, 1400), startsInRange(pattern, 840, 1680));
    }

    @Test
    void ratchetWidthSelectsMultipleStructuralRegionsDeterministically() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, NestedRhythmGenerator.TupletCoverage.NONE, 0, 4, 2, 0, 0.6, 100, 0, 0));

        assertEquals(4, startsInRange(pattern, 0, 420).size());
        assertEquals(List.of(420), startsInRange(pattern, 420, 840));
        assertEquals(4, startsInRange(pattern, 840, 1260).size());
        assertEquals(List.of(1260), startsInRange(pattern, 1260, NestedRhythmGenerator.FINE_STEPS_PER_BAR));
    }

    @Test
    void ratchetPhaseRotatesChosenRegionsWithoutChangingCount() {
        final NestedRhythmPattern base = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, NestedRhythmGenerator.TupletCoverage.NONE, 0, 4, 1, 0, 0.6, 100, 0, 0));
        final NestedRhythmPattern rotated = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, NestedRhythmGenerator.TupletCoverage.NONE, 0, 4, 1, 1, 0.6, 100, 0, 0));

        assertEquals(4, startsInRange(base, 0, 420).size());
        assertEquals(List.of(420, 840, 1260), startsOutsideRange(base, 0, 420));
        assertEquals(4, startsInRange(rotated, 840, 1260).size());
        assertEquals(List.of(0, 420, 1260), startsOutsideRange(rotated, 840, 1260));
    }

    @Test
    void increasingDensityNeverAddsNewPositionsBeyondStructure() {
        final NestedRhythmPattern sparse = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 7, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 4, 2, 0, 0.6, 100, 0, 0));
        final NestedRhythmPattern dense = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 7, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 4, 2, 0, 0.6, 100, 0, 0));

        final Set<Integer> sparseStarts = starts(sparse).stream().collect(Collectors.toSet());
        final Set<Integer> denseStarts = starts(dense).stream().collect(Collectors.toSet());
        assertTrue(denseStarts.containsAll(sparseStarts));
    }

    @Test
    void velocityRotationAdvancesOneHitAtATime() {
        final NestedRhythmPattern rotation0 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, NestedRhythmGenerator.TupletCoverage.NONE, 0, 0, 1, 0, 0.6, 100, 0, 0));
        final NestedRhythmPattern rotation1 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, NestedRhythmGenerator.TupletCoverage.NONE, 0, 0, 1, 0, 0.6, 100, 1, 0));

        assertEquals(starts(rotation0), starts(rotation1));
        assertEquals(velocities(rotation0).get(1), velocities(rotation1).get(0));
        assertEquals(velocities(rotation0).get(2), velocities(rotation1).get(1));
    }

    @Test
    void velocityRotationCanMoveStrongestHit() {
        final NestedRhythmPattern rotation0 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 5, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 4, 2, 0, 0.6, 100, 0, 0));
        final NestedRhythmPattern rotation1 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 5, NestedRhythmGenerator.TupletCoverage.BACK_HALF, 0, 4, 2, 0, 0.6, 100, 1, 0));

        assertNotEquals(startOfMaxVelocity(rotation0), startOfMaxVelocity(rotation1));
    }

    private NestedRhythmGenerator.Settings defaultSettings() {
        return new NestedRhythmGenerator.Settings(
                60, 0.0, 0, NestedRhythmGenerator.TupletCoverage.NONE, 0, 0, 1, 0, 0.6, 100, 0, 0);
    }

    private int startOfMaxVelocity(final NestedRhythmPattern pattern) {
        return pattern.events().stream()
                .max(Comparator.comparingInt(NestedRhythmPattern.PulseEvent::velocity))
                .orElseThrow()
                .fineStart();
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

    private List<Integer> startsOutsideRange(final NestedRhythmPattern pattern, final int startInclusive,
                                             final int endExclusive) {
        return pattern.events().stream()
                .filter(event -> event.fineStart() < startInclusive || event.fineStart() >= endExclusive)
                .map(NestedRhythmPattern.PulseEvent::fineStart)
                .toList();
    }
}
