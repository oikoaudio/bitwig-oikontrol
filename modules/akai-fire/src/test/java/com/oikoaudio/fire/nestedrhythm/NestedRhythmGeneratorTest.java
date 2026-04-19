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
                60, 0.75, 5, 1, 1, 4, 2, 1, 0.7, 100, 2, 1,
                4, 4, 1);

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
    void multipleBarsKeepMeterAnchorsAcrossTheWholePhrase() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                4, 4, 2));

        assertEquals(List.of(0, 420, 840, 1260, 1680, 2100, 2520, 2940), starts(pattern));
    }

    @Test
    void meterNumeratorAndDenominatorDefineAnchorGrid() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                7, 8, 1));

        assertEquals(List.of(0, 210, 420, 630, 840, 1050, 1260), starts(pattern));
    }

    @Test
    void fourFourTupletCountsStayOnThreeFiveSeven() {
        assertEquals(List.of(0, 3, 5, 7),
                java.util.Arrays.stream(NestedRhythmGenerator.supportedTupletCounts(4, 4, 1))
                        .boxed()
                        .toList());
    }

    @Test
    void fiveFourTupletCountsPreferNonNativeDivisions() {
        assertEquals(List.of(0, 3, 4, 6, 7),
                java.util.Arrays.stream(NestedRhythmGenerator.supportedTupletCounts(5, 4, 1))
                        .boxed()
                        .toList());
    }

    @Test
    void multiBarTupletCoverClaimsConsecutiveHalfBars() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 2, 2, 0, 1, 0, 1.0, 100, 0, 0,
                4, 4, 2));

        assertEquals(List.of(0, 420, 840, 1260, 1680, 2240, 2800), starts(pattern));
    }

    @Test
    void multiBarRatchetWidthDistributesAcrossPhraseInsteadOfRepeatingPerBar() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 2, 0, 1.0, 100, 0, 0,
                4, 4, 2));

        assertEquals(List.of(0, 105, 210, 315), startsInRange(pattern, 0, 420));
        assertEquals(List.of(420, 840, 1260), startsInRange(pattern, 420, 1680));
        assertEquals(List.of(1680, 1785, 1890, 1995), startsInRange(pattern, 1680, 2100));
        assertEquals(List.of(2100, 2520, 2940), startsInRange(pattern, 2100, 3360));
    }

    @Test
    void densityMinimumKeepsAnchorsAndRequiredSubdivisionLeads() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 7, 1, 1, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0, 420, 840), starts(pattern));
    }

    @Test
    void backHalfTupletLeavesFrontHalfAnchorsInPlace() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 1, 0, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0, 420), startsInRange(pattern, 0, 840));
        assertEquals(List.of(840, 1120, 1400),
                startsInRange(pattern, 840, NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void tupletPhaseMovesHalfBarTupletToFrontHalf() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 0, 0, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0, 280, 560), startsInRange(pattern, 0, 840));
        assertEquals(List.of(840, 1260), startsInRange(pattern, 840, NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void ratchetOverridesTupletInsideClaimedBeatSpan() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 1, 4, 1, 2, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0), startsInRange(pattern, 0, 420));
        assertEquals(List.of(420), startsInRange(pattern, 420, 840));
        assertEquals(List.of(840, 945, 1050, 1155), startsInRange(pattern, 840, 1260));
        assertEquals(List.of(1400), startsInRange(pattern, 1260, 1680));
    }

    @Test
    void ratchetWidthSelectsMultipleStructuralRegionsDeterministically() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(4, startsInRange(pattern, 0, 420).size());
        assertEquals(List.of(420), startsInRange(pattern, 420, 840));
        assertEquals(4, startsInRange(pattern, 840, 1260).size());
        assertEquals(List.of(1260), startsInRange(pattern, 1260, NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void ratchetPhaseRotatesChosenRegionsWithoutChangingCount() {
        final NestedRhythmPattern base = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));
        final NestedRhythmPattern rotated = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 1, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(4, startsInRange(base, 0, 420).size());
        assertEquals(List.of(420, 840, 1260), startsOutsideRange(base, 0, 420));
        assertEquals(4, startsInRange(rotated, 420, 840).size());
        assertEquals(List.of(0, 840, 1260), startsOutsideRange(rotated, 420, 840));
    }

    @Test
    void ratchetPhaseInFiveFourCanLandOnEvenBeats() {
        final NestedRhythmPattern rotated = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 1, 0.6, 100, 0, 0,
                5, 4, 1));

        assertEquals(4, startsInRange(rotated, 420, 840).size());
        assertEquals(List.of(0, 840, 1260, 1680), startsOutsideRange(rotated, 420, 840));
    }

    @Test
    void increasingDensityNeverAddsNewPositionsBeyondStructure() {
        final NestedRhythmPattern sparse = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 7, 1, 1, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));
        final NestedRhythmPattern dense = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 7, 1, 1, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        final Set<Integer> sparseStarts = starts(sparse).stream().collect(Collectors.toSet());
        final Set<Integer> denseStarts = starts(dense).stream().collect(Collectors.toSet());
        assertTrue(denseStarts.containsAll(sparseStarts));
    }

    @Test
    void velocityRotationAdvancesOneHitAtATime() {
        final NestedRhythmPattern rotation0 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));
        final NestedRhythmPattern rotation1 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 0.6, 100, 1, 0,
                4, 4, 1));

        assertEquals(starts(rotation0), starts(rotation1));
        assertEquals(velocities(rotation0).get(1), velocities(rotation1).get(0));
        assertEquals(velocities(rotation0).get(2), velocities(rotation1).get(1));
    }

    @Test
    void velocityRotationCanMoveStrongestHit() {
        final NestedRhythmPattern rotation0 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 5, 1, 1, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));
        final NestedRhythmPattern rotation1 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 5, 1, 1, 4, 2, 0, 0.6, 100, 1, 0,
                4, 4, 1));

        assertNotEquals(startOfMaxVelocity(rotation0), startOfMaxVelocity(rotation1));
    }

    private NestedRhythmGenerator.Settings defaultSettings() {
        return new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1);
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
