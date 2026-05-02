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
    void rateTwoAddsOptionalBasePulsesBetweenBeatAnchors() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1, 2.0));

        assertEquals(List.of(0, 210, 420, 630, 840, 1050, 1260, 1470), starts(pattern));
    }

    @Test
    void rateTwoThinsBackToBeatAnchorsAtMinimumDensity() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1, 2.0));

        assertEquals(List.of(0, 420, 840, 1260), starts(pattern));
    }

    @Test
    void slowRateSpreadsRequiredAnchorsAcrossThePhrase() {
        final NestedRhythmPattern halfRate = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1, 0.5));
        final NestedRhythmPattern quarterRate = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 4, 0.25));

        assertEquals(List.of(0, 840), starts(halfRate));
        assertEquals(List.of(0, 1680, 3360, 5040), starts(quarterRate));
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
    void ratchetTargetCountIncludesTupletParentCells() {
        assertEquals(4, NestedRhythmGenerator.ratchetParentRegionCount(
                4, 4, 1, 0, 0, 0, 0.0));
        assertEquals(8, NestedRhythmGenerator.ratchetParentRegionCount(
                4, 4, 1, 0, 0, 0, 0.0, 2.0));
        assertEquals(5, NestedRhythmGenerator.ratchetParentRegionCount(
                4, 4, 1, 3, 1, 1, 0.0));
    }

    @Test
    void rateIncreaseScalesRatchetCellsAndTupletSpans() {
        final NestedRhythmPattern ratchetRateOne = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 4, 1.0));
        final NestedRhythmPattern ratchetRateTwo = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 4, 2.0));
        final NestedRhythmPattern tupletRateTwo = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1, 2.0));

        final List<Integer> rateOneRatchets = startsWithRoles(ratchetRateOne, Set.of(
                NestedRhythmPattern.Role.RATCHET_LEAD,
                NestedRhythmPattern.Role.RATCHET_INTERIOR));
        final List<Integer> rateTwoRatchets = startsWithRoles(ratchetRateTwo, Set.of(
                NestedRhythmPattern.Role.RATCHET_LEAD,
                NestedRhythmPattern.Role.RATCHET_INTERIOR));

        assertEquals(4, rateOneRatchets.size());
        assertEquals(4, rateTwoRatchets.size());
        assertTrue(rateOneRatchets.get(3) - rateOneRatchets.get(0) < 420);
        assertTrue(rateTwoRatchets.get(3) - rateTwoRatchets.get(0) < 210);
        assertEquals(List.of(840, 980, 1120), startsWithRoles(tupletRateTwo, Set.of(
                NestedRhythmPattern.Role.TUPLET_LEAD,
                NestedRhythmPattern.Role.TUPLET_INTERIOR)));
    }

    @Test
    void multiBarTupletTargetsSplitSelectedHalfBarsIndependently() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 2, 2, 0, 1, 0, 1.0, 100, 0, 0,
                4, 4, 2));

        assertEquals(List.of(0, 280, 560, 840, 1260, 1680, 2100, 2520, 2800, 3080), starts(pattern));
    }

    @Test
    void multiBarRatchetTargetsUseWholePhraseOrderWithoutRepeatingOneBarPattern() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 2, 0, 1.0, 100, 0, 0,
                4, 4, 2));

        final List<Integer> ratchets = startsWithRoles(pattern, Set.of(
                NestedRhythmPattern.Role.RATCHET_LEAD,
                NestedRhythmPattern.Role.RATCHET_INTERIOR));

        assertEquals(8, ratchets.size());
        assertTrue(ratchets.stream().anyMatch(start -> start < NestedRhythmGenerator.fineStepsPerBar(4, 4)));
        assertTrue(ratchets.stream().anyMatch(start -> start >= NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void ratchetTargetPhaseDefaultsToPriorityOrderedParentRegions() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 4, 0, 1.0, 100, 0, 0,
                4, 4, 2));

        assertEquals(4, startsInRange(pattern, 0, 420).size());
        assertEquals(4, startsInRange(pattern, 1680, 2100).size());
        assertEquals(4, startsInRange(pattern, 840, 1260).size());
        assertEquals(4, startsInRange(pattern, 2940, 3360).size());
        assertEquals(List.of(420, 1260, 2100, 2520), startsOutsideRanges(pattern,
                List.of(new Range(0, 420), new Range(840, 1260), new Range(1680, 2100),
                        new Range(2940, 3360))));
    }

    @Test
    void densityMinimumKeepsSignatureCellWhenTransformsClaimAnchors() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 7, 1, 0, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertTrue(pattern.events().size() >= 2);
        assertTrue(pattern.events().stream().anyMatch(event -> isRatchet(event.role())));
        assertTrue(pattern.events().stream().anyMatch(event -> isTuplet(event.role())));
        assertTrue(starts(pattern).stream().allMatch(start -> start >= 0
                && start < NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void densityMinimumPreservesGeneratedIdentityOverBeatAnchors() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 3, 1, 0, 4, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertTrue(pattern.events().stream().anyMatch(event -> isTuplet(event.role())));
    }

    @Test
    void densityMinimumStillKeepsNotesWhenTupletsAndRatchetsClaimAllAnchors() {
        final NestedRhythmPattern unclustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 3, 4, 0, 2, 3, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 2));
        final NestedRhythmPattern lightlyClustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 3, 4, 0, 2, 3, 0, 1.0, 100, 0, 0,
                0.05, 4, 4, 2));

        assertTrue(unclustered.events().size() >= 2);
        assertTrue(lightlyClustered.events().size() >= 2);
        assertTrue(unclustered.events().stream().allMatch(event -> event.fineStart() >= 0
                && event.fineStart() < NestedRhythmGenerator.fineStepsPerBar(4, 4) * 2));
    }

    @Test
    void densityMinimumKeepsGeneratedTupletGestureInsteadOfOnlyAnchors() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 3, 1, 0, 4, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertTrue(pattern.events().stream().anyMatch(event -> isTuplet(event.role())));
    }

    @Test
    void backHalfTupletLeavesFrontHalfAnchorsInPlace() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 0, 0, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0, 420), startsInRange(pattern, 0, 840));
        assertEquals(List.of(840, 1120, 1400),
                startsInRange(pattern, 840, NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void tupletTargetPhaseMovesHalfBarTupletToFrontHalf() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 1, 0, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0, 280, 560), startsInRange(pattern, 0, 840));
        assertEquals(List.of(840, 1260), startsInRange(pattern, 840, NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void multiBarTupletTargetsUseWholePhraseOrder() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 2, 0, 0, 1, 0, 0.6, 100, 0, 0,
                4, 4, 2));
        final int barFineSteps = NestedRhythmGenerator.fineStepsPerBar(4, 4);

        assertEquals(List.of(840, 1120, 1400), startsInRange(pattern, 840, barFineSteps));
        assertEquals(List.of(1680, 1960, 2240), startsInRange(pattern, barFineSteps, barFineSteps + 840));
    }

    @Test
    void ratchetsComplementTupletTargetsInsteadOfOnlyNestingInsideThem() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 2, 0, 2, 4, 0, 0.6, 100, 0, 0,
                4, 4, 2));

        assertTrue(startsWithRoles(pattern, Set.of(
                NestedRhythmPattern.Role.RATCHET_LEAD,
                NestedRhythmPattern.Role.RATCHET_INTERIOR)).stream().anyMatch(start -> start < 840),
                starts(pattern).toString());
        assertTrue(startsWithRoles(pattern, Set.of(
                NestedRhythmPattern.Role.RATCHET_LEAD,
                NestedRhythmPattern.Role.RATCHET_INTERIOR)).stream().anyMatch(start -> start >= 840),
                starts(pattern).toString());
    }

    @Test
    void ratchetTargetsEnterTupletCellsAfterComplementaryBeatCell() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 0, 2, 3, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0, 210, 840, 980, 1400, 1540), startsWithRoles(pattern, Set.of(
                NestedRhythmPattern.Role.RATCHET_LEAD,
                NestedRhythmPattern.Role.RATCHET_INTERIOR)));
    }

    @Test
    void ratchetSplitsTupletCellsAfterTupletPlacement() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 0, 4, 1, 1, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0), startsInRange(pattern, 0, 420));
        assertEquals(List.of(420), startsInRange(pattern, 420, 840));
        assertEquals(List.of(840, 910, 980, 1050), startsInRange(pattern, 840, 1120));
        assertEquals(List.of(1120), startsInRange(pattern, 1120, 1400));
        assertEquals(List.of(1400), startsInRange(pattern, 1400, 1680));
    }

    @Test
    void ratchetTargetsSelectMultipleStructuralRegionsDeterministically() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(4, startsInRange(pattern, 0, 420).size());
        assertEquals(4, startsInRange(pattern, 420, 840).size());
        assertEquals(List.of(840), startsInRange(pattern, 840, 1260));
        assertEquals(List.of(1260), startsInRange(pattern, 1260, NestedRhythmGenerator.fineStepsPerBar(4, 4)));
    }

    @Test
    void ratchetTargetPhaseRotatesChosenRegionsWithoutChangingCount() {
        final NestedRhythmPattern base = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 0, 0.6, 100, 0, 0,
                4, 4, 1));
        final NestedRhythmPattern rotated = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 1, 0.6, 100, 0, 0,
                4, 4, 1));

        assertEquals(4, startsInRange(base, 420, 840).size());
        assertEquals(List.of(0, 840, 1260), startsOutsideRange(base, 420, 840));
        assertEquals(4, startsInRange(rotated, 0, 420).size());
        assertEquals(List.of(420, 840, 1260), startsOutsideRange(rotated, 0, 420));
    }

    @Test
    void ratchetTargetPhaseInFiveFourCanLandOnEvenBeats() {
        final NestedRhythmPattern rotated = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 1, 0, 0.6, 100, 0, 0,
                5, 4, 1));

        assertEquals(4, startsInRange(rotated, 420, 840).size());
        assertEquals(List.of(0, 840, 1260, 1680), startsOutsideRange(rotated, 420, 840));
    }

    @Test
    void increasingDensityNeverAddsNewPositionsBeyondStructure() {
        final NestedRhythmPattern sparse = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 7, 1, 0, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));
        final NestedRhythmPattern dense = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 7, 1, 0, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));

        final Set<Integer> sparseStarts = starts(sparse).stream().collect(Collectors.toSet());
        final Set<Integer> denseStarts = starts(dense).stream().collect(Collectors.toSet());
        assertTrue(denseStarts.containsAll(sparseStarts));
    }

    @Test
    void densitySelectionIsMonotonicWhenClustered() {
        Set<Integer> previousOrders = Set.of();
        for (double density = 0.0; density <= 1.0001; density += 0.05) {
            final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                    60, density, 7, 1, 0, 4, 2, 0, 1.0, 100, 0, 0,
                    1.0, 4, 4, 1));
            final Set<Integer> currentOrders = orders(pattern);

            assertTrue(currentOrders.containsAll(previousOrders),
                    "density " + density + " lost " + previousOrders + " from " + currentOrders);
            previousOrders = currentOrders;
        }
    }

    @Test
    void clusteredDensityIncreaseDoesNotRedistributeAlreadyRetainedHits() {
        final NestedRhythmPattern lowerDensity = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.5875, 3, 2, 0, 6, 6, 0, 1.0, 100, 0, 0,
                0.65, 4, 4, 2));
        final NestedRhythmPattern higherDensity = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.6, 3, 2, 0, 6, 6, 0, 1.0, 100, 0, 0,
                0.65, 4, 4, 2));

        assertTrue(orders(higherDensity).containsAll(orders(lowerDensity)),
                lowerDensity.events().size() + " -> " + higherDensity.events().size());
        for (final NestedRhythmPattern.PulseEvent retained : lowerDensity.events()) {
            final int shiftedStart = startForOrder(higherDensity, retained.order());
            final int thirtySecondStep = (int) Math.ceil(NestedRhythmGenerator.FINE_STEPS_PER_WHOLE / 32.0);
            assertTrue(Math.abs(retained.fineStart() - shiftedStart) <= thirtySecondStep,
                    "order " + retained.order() + " jumped from " + retained.fineStart() + " to " + shiftedStart);
        }
    }

    @Test
    void loweringDensityDoesNotStretchRetainedNoteDurations() {
        final NestedRhythmGenerator.Settings fullSettings = new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 8, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1);
        final NestedRhythmGenerator.Settings sparseSettings = new NestedRhythmGenerator.Settings(
                60, 0.25, 0, 0, 0, 8, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1);
        final NestedRhythmPattern full = generator.generate(fullSettings);
        final NestedRhythmPattern sparse = generator.generate(sparseSettings);

        for (final NestedRhythmPattern.PulseEvent event : sparse.events()) {
            assertEquals(durationAt(full, event.fineStart()), event.duration());
        }
    }

    @Test
    void densityThinningPrefersAdjacentOptionalPairs() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.25, 0, 0, 0, 8, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1));
        final List<Integer> retainedRatchetStarts = startsInRange(pattern, 420, 840);

        assertTrue(retainedRatchetStarts.size() >= 2);
        assertTrue(hasAdjacentPairWithin(retainedRatchetStarts, 60));
    }

    @Test
    void displayedMidDensityKeepsReadableRatchetOnlyPhrase() {
        final double internalDensityForDisplayedFortySixPercent = (0.46 - 0.20) / 0.80;
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, internalDensityForDisplayedFortySixPercent, 0, 0, 0, 2, 4, 0, 1.0, 100, 0, 0,
                4, 4, 2));

        assertTrue(pattern.events().size() >= 6, starts(pattern).toString());
        assertTrue(pattern.events().stream()
                .map(event -> event.fineStart() / NestedRhythmGenerator.FINE_STEPS_PER_BEAT)
                .collect(Collectors.toSet())
                .size() >= 3, starts(pattern).toString());
    }

    @Test
    void densityPriorityInterleavesTupletLeadsWithRatchetDetails() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.36, 7, 1, 0, 4, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1));

        assertEquals(List.of(0, 105, 840, 960, 1440, 1560), starts(pattern));
    }

    @Test
    void mediumDensityKeepsNestedTupletGestureAlongsideStraightRatchets() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.45, 3, 1, 0, 2, 4, 0, 1.0, 100, 0, 0,
                4, 4, 1));

        assertTrue(startsInRange(pattern, 0, 840).size() >= 2, starts(pattern).toString());
        assertTrue(startsInRange(pattern, 840, 1400).size() >= 2, starts(pattern).toString());
        assertTrue(pattern.events().stream().anyMatch(event -> isRatchet(event.role())));
        assertTrue(pattern.events().stream().anyMatch(event -> isTuplet(event.role())));
    }

    @Test
    void clusterConcentratesSparseOptionalPairsTowardPhraseEnd() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 4, 4, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));
        final List<Integer> retainedStarts = starts(pattern);

        assertTrue(retainedStarts.size() >= 3);
        assertTrue(retainedStarts.stream().allMatch(start -> start >= 1260));
    }

    @Test
    void clusterDoesNotWrapAroundToTheStartOfTheClip() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 4, 4, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));
        final List<Integer> retainedStarts = starts(pattern);

        assertTrue(retainedStarts.size() >= 3);
        assertTrue(retainedStarts.stream().allMatch(start -> start >= 1260));
    }

    @Test
    void clusterDoesNotDropAnchorsWhenThereAreNoOptionalHitsToTrade() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.5, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        assertEquals(4, starts(pattern).size());
        assertTrue(starts(pattern).stream().allMatch(start -> start >= 1260));
    }

    @Test
    void halfClusterWithSparsePhraseStartsInBackHalfWhenThereIsRoom() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.45, 3, 1, 0, 2, 3, 0, 1.0, 100, 0, 0,
                0.5, 4, 4, 1));

        assertTrue(starts(pattern).get(0) >= 840, starts(pattern).toString());
    }

    @Test
    void clusterSnapsAnchorsToSixteenthGrid() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.5, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        assertTrue(pattern.events().stream()
                .allMatch(event -> isWholeDivisionGridLine(event.fineStart(), 16)));
    }

    @Test
    void clusterPrefersSixteenthGridForSubdivisionsWhenThereIsSpace() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 4, 4, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        assertTrue(pattern.events().stream()
                .filter(event -> !isAnchor(event.role()))
                .allMatch(event -> isWholeDivisionGridLine(event.fineStart(), 16)),
                pattern.events().toString());
    }

    @Test
    void clusterFallsBackToThirtySecondGridForDenseSubdivisions() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.35, 7, 1, 0, 4, 2, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        assertTrue(pattern.events().stream()
                .filter(event -> !isAnchor(event.role()))
                .allMatch(event -> isWholeDivisionGridLine(event.fineStart(), 32)),
                pattern.events().toString());
        assertTrue(pattern.events().stream()
                .filter(event -> !isAnchor(event.role()))
                .anyMatch(event -> !isWholeDivisionGridLine(event.fineStart(), 16)),
                pattern.events().toString());
    }

    @Test
    void clusterShortensDurationsAfterGridSnapping() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.65, 7, 1, 0, 8, 2, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        for (int index = 0; index < pattern.events().size(); index++) {
            final NestedRhythmPattern.PulseEvent current = pattern.events().get(index);
            final NestedRhythmPattern.PulseEvent next = pattern.events().get((index + 1) % pattern.events().size());
            final int gap = next.fineStart() > current.fineStart()
                    ? next.fineStart() - current.fineStart()
                    : NestedRhythmGenerator.fineStepsPerBar(4, 4) - current.fineStart() + next.fineStart();

            assertTrue(current.duration() <= gap);
        }
    }

    @Test
    void lastEventDurationDoesNotOverrunClipEnd() {
        final NestedRhythmGenerator.Settings settings = new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1);
        final NestedRhythmPattern pattern = generator.generate(settings);
        final int clipEnd = NestedRhythmGenerator.fineStepsPerBar(4, 4);

        for (final NestedRhythmPattern.PulseEvent event : pattern.events()) {
            assertTrue(event.fineStart() + event.duration() <= clipEnd,
                    event.fineStart() + " + " + event.duration() + " overran " + clipEnd);
        }
    }

    @Test
    void clusteredThinningKeepsFullStructureDurationCaps() {
        final NestedRhythmPattern fullClustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 7, 1, 0, 8, 2, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));
        final NestedRhythmPattern sparseClustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 7, 1, 0, 8, 2, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        for (final NestedRhythmPattern.PulseEvent event : sparseClustered.events()) {
            final int fullDuration = durationAtOrder(fullClustered, event.order());
            final int clipEndCap = NestedRhythmGenerator.fineStepsPerBar(4, 4) - event.fineStart();
            assertTrue(event.duration() <= Math.max(fullDuration, clipEndCap),
                    "order " + event.order() + " stretched from " + fullDuration + " to " + event.duration());
            assertTrue(event.fineStart() + event.duration() <= NestedRhythmGenerator.fineStepsPerBar(4, 4));
        }
    }

    @Test
    void partialClusterDoesNotStretchSparseWraparoundDuration() {
        final NestedRhythmPattern unclustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1));
        final NestedRhythmPattern clustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.0, 0, 0, 0, 0, 1, 0, 1.0, 100, 0, 0,
                0.5, 4, 4, 1));

        for (final NestedRhythmPattern.PulseEvent event : clustered.events()) {
            assertTrue(event.duration() <= durationAtOrder(unclustered, event.order()),
                    "order " + event.order() + " stretched from "
                            + durationAtOrder(unclustered, event.order()) + " to " + event.duration());
        }
    }

    @Test
    void clusterPreservesTheDensityRetainedHitCount() {
        final NestedRhythmPattern unclustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.25, 0, 0, 0, 4, 1, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1));
        final NestedRhythmPattern clustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.25, 0, 0, 0, 4, 1, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        assertEquals(starts(unclustered).size(), starts(clustered).size());
    }

    @Test
    void clusteredTupletsAddDensityInsteadOfReplacingRatchetPhrase() {
        final NestedRhythmPattern ratchetsOnly = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.25, 0, 0, 0, 4, 2, 0, 1.0, 100, 0, 0,
                0.65, 4, 4, 1));
        final NestedRhythmPattern withTuplets = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.25, 7, 1, 0, 4, 2, 0, 1.0, 100, 0, 0,
                0.65, 4, 4, 1));

        assertTrue(withTuplets.events().size() > ratchetsOnly.events().size());
        assertTrue(withTuplets.events().stream().anyMatch(event -> isRatchet(event.role())));
        assertTrue(withTuplets.events().stream().anyMatch(event -> isTuplet(event.role())));
    }

    @Test
    void fullClusterKeepsMixedRatchetAndTupletMaterial() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.4, 7, 1, 0, 4, 2, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        assertTrue(starts(pattern).size() >= 6);
        assertTrue(starts(pattern).stream().allMatch(start -> start >= 1260));
        assertTrue(pattern.events().stream().anyMatch(event -> isRatchet(event.role())));
        assertTrue(pattern.events().stream().anyMatch(event -> isTuplet(event.role())));
    }

    @Test
    void clusteringKeepsVelocityContourAttachedToFullStructurePositions() {
        final NestedRhythmPattern full = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 4, 4, 0, 1.0, 100, 0, 0,
                0.0, 4, 4, 1));
        final NestedRhythmPattern clustered = generator.generate(new NestedRhythmGenerator.Settings(
                60, 0.25, 0, 0, 0, 4, 4, 0, 1.0, 100, 0, 0,
                1.0, 4, 4, 1));

        for (final NestedRhythmPattern.PulseEvent event : clustered.events()) {
            assertEquals(velocityAtOrder(full, event.order()), event.velocity());
        }
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
                60, 1.0, 5, 1, 0, 4, 2, 0, 0.6, 100, 0, 0,
                4, 4, 1));
        final NestedRhythmPattern rotation1 = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 5, 1, 0, 4, 2, 0, 0.6, 100, 1, 0,
                4, 4, 1));

        assertNotEquals(startOfMaxVelocity(rotation0), startOfMaxVelocity(rotation1));
    }

    @Test
    void lowerRatchetDivisionsStillReceiveOuterContour() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 2, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1));
        final List<Integer> ratchet = velocitiesInRange(pattern, 420, 840);

        assertEquals(2, ratchet.size());
        assertTrue(ratchet.get(0) > ratchet.get(1));
    }

    @Test
    void threeTupletsReceiveOuterContourWithoutLosingTheirExistingLeadShape() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 1, 0, 0, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1));
        final List<Integer> tuplet = velocitiesInRange(pattern, 840, 1680);

        assertEquals(3, tuplet.size());
        assertTrue(tuplet.get(0) > tuplet.get(2));
        assertTrue(tuplet.get(1) < tuplet.get(2));
    }

    @Test
    void highRatchetCountsKeepInnerAlternationWhileRisingIntoNextAnchor() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 8, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1));
        final List<Integer> ratchet = velocitiesInRange(pattern, 420, 840);
        final int nextAnchor = velocitiesInRange(pattern, 840, 1260).get(0);

        assertEquals(8, ratchet.size());
        assertTrue(ratchet.get(0) > ratchet.get(7));
        assertTrue(ratchet.get(7) < nextAnchor);
        assertTrue(ratchet.get(2) < ratchet.get(4));
        assertTrue(ratchet.get(4) < ratchet.get(6));
        assertTrue(ratchet.get(1) < ratchet.get(3));
        assertTrue(ratchet.get(3) < ratchet.get(5));
        assertTrue(ratchet.get(5) < ratchet.get(7));
    }

    @Test
    void ratchetTargetsShareOuterContourAcrossTheSelectedPhrase() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 0, 0, 0, 2, 4, 0, 1.0, 100, 0, 0,
                4, 4, 1));

        assertTrue(velocityAt(pattern, 1470) > velocityAt(pattern, 210));
    }

    @Test
    void repeatedRatchetLeadsInsideLongerPhraseArePulledIntoTheOuterContour() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 3, 2, 0, 2, 6, 0, 1.0, 100, 0, 0,
                4, 4, 1));

        assertTrue(velocityAt(pattern, 280) < velocityAt(pattern, 0));
        assertTrue(velocityAt(pattern, 1400) > velocityAt(pattern, 280));
        assertTrue(velocityAt(pattern, 1540) > velocityAt(pattern, 140));
    }

    @Test
    void highTupletCountsKeepInnerAlternationWhileRisingThroughTheClaimedSpan() {
        final NestedRhythmPattern pattern = generator.generate(new NestedRhythmGenerator.Settings(
                60, 1.0, 7, 1, 0, 0, 1, 0, 1.0, 100, 0, 0,
                4, 4, 1));
        final List<Integer> tuplet = velocitiesInRange(pattern, 840, 1680);

        assertEquals(7, tuplet.size());
        assertTrue(tuplet.get(0) > tuplet.get(6));
        assertTrue(tuplet.get(2) < tuplet.get(4));
        assertTrue(tuplet.get(4) < tuplet.get(6));
        assertTrue(tuplet.get(1) < tuplet.get(3));
        assertTrue(tuplet.get(3) < tuplet.get(5));
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

    private boolean hasAdjacentPairWithin(final List<Integer> starts,
                                          final int maximumDistance) {
        for (int index = 0; index + 1 < starts.size(); index++) {
            if (starts.get(index + 1) - starts.get(index) <= maximumDistance) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> startsWithRoles(final NestedRhythmPattern pattern,
                                          final Set<NestedRhythmPattern.Role> roles) {
        return pattern.events().stream()
                .filter(event -> roles.contains(event.role()))
                .map(NestedRhythmPattern.PulseEvent::fineStart)
                .toList();
    }

    private Set<Integer> orders(final NestedRhythmPattern pattern) {
        return pattern.events().stream()
                .map(NestedRhythmPattern.PulseEvent::order)
                .collect(Collectors.toSet());
    }

    private boolean isAnchor(final NestedRhythmPattern.Role role) {
        return role == NestedRhythmPattern.Role.PRIMARY_ANCHOR
                || role == NestedRhythmPattern.Role.SECONDARY_ANCHOR;
    }

    private boolean isRatchet(final NestedRhythmPattern.Role role) {
        return role == NestedRhythmPattern.Role.RATCHET_LEAD
                || role == NestedRhythmPattern.Role.RATCHET_INTERIOR;
    }

    private boolean isTuplet(final NestedRhythmPattern.Role role) {
        return role == NestedRhythmPattern.Role.TUPLET_LEAD
                || role == NestedRhythmPattern.Role.TUPLET_INTERIOR;
    }

    private boolean isWholeDivisionGridLine(final int fineStart, final int divisions) {
        final int nearestIndex = (int) Math.round(fineStart * divisions
                / (double) NestedRhythmGenerator.FINE_STEPS_PER_WHOLE);
        final int nearestGridLine = (int) Math.round(nearestIndex
                * NestedRhythmGenerator.FINE_STEPS_PER_WHOLE / (double) divisions);
        return fineStart == nearestGridLine;
    }

    private List<Integer> velocities(final NestedRhythmPattern pattern) {
        return pattern.events().stream().map(NestedRhythmPattern.PulseEvent::velocity).toList();
    }

    private int durationAt(final NestedRhythmPattern pattern, final int fineStart) {
        return pattern.events().stream()
                .filter(event -> event.fineStart() == fineStart)
                .findFirst()
                .orElseThrow()
                .duration();
    }

    private int durationAtOrder(final NestedRhythmPattern pattern, final int order) {
        return pattern.events().stream()
                .filter(event -> event.order() == order)
                .findFirst()
                .orElseThrow()
                .duration();
    }

    private int startForOrder(final NestedRhythmPattern pattern, final int order) {
        return pattern.events().stream()
                .filter(event -> event.order() == order)
                .findFirst()
                .orElseThrow()
                .fineStart();
    }

    private int velocityAt(final NestedRhythmPattern pattern, final int fineStart) {
        return pattern.events().stream()
                .filter(event -> event.fineStart() == fineStart)
                .findFirst()
                .orElseThrow()
                .velocity();
    }

    private int velocityAtOrder(final NestedRhythmPattern pattern, final int order) {
        return pattern.events().stream()
                .filter(event -> event.order() == order)
                .findFirst()
                .orElseThrow()
                .velocity();
    }

    private List<Integer> velocitiesInRange(final NestedRhythmPattern pattern, final int startInclusive,
                                            final int endExclusive) {
        return pattern.events().stream()
                .filter(event -> event.fineStart() >= startInclusive && event.fineStart() < endExclusive)
                .map(NestedRhythmPattern.PulseEvent::velocity)
                .toList();
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

    private List<Integer> startsOutsideRanges(final NestedRhythmPattern pattern, final List<Range> ranges) {
        return pattern.events().stream()
                .filter(event -> ranges.stream().noneMatch(range -> range.contains(event.fineStart())))
                .map(NestedRhythmPattern.PulseEvent::fineStart)
                .toList();
    }

    private record Range(int startInclusive, int endExclusive) {
        private boolean contains(final int fineStart) {
            return fineStart >= startInclusive && fineStart < endExclusive;
        }
    }
}
