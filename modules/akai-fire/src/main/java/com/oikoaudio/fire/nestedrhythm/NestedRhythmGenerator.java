package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class NestedRhythmGenerator {
    public static final int DEFAULT_BEATS_PER_BAR = 4;
    public static final int DEFAULT_BEAT_DENOMINATOR = 4;
    public static final int FINE_STEPS_PER_QUARTER = 420;
    public static final int FINE_STEPS_PER_WHOLE = FINE_STEPS_PER_QUARTER * 4;
    public static final int BEATS_PER_BAR = DEFAULT_BEATS_PER_BAR;
    public static final int FINE_STEPS_PER_BEAT = FINE_STEPS_PER_QUARTER;
    public static final int FINE_STEPS_PER_BAR = FINE_STEPS_PER_WHOLE;
    public static final double DEFAULT_VELOCITY_DEPTH = 1.75;
    public static final double MIN_VELOCITY_DEPTH = 0.44;
    public static final double MAX_VELOCITY_DEPTH = 2.0;

    private static final int MAX_BARS = 4;
    private static final int MAX_NUMERATOR = 16;
    private static final double GENERATED_DENSITY_FLOOR = 0.20;
    private static final int[] SUPPORTED_DENOMINATORS = {2, 4, 8, 16};
    private static final int[] TUPLET_COUNT_CANDIDATES = {0, 3, 4, 5, 6, 7};
    private static final int[] SUPPORTED_RATCHET_DIVISIONS = {0, 2, 3, 4, 5, 6, 7, 8};
    private static final double[] SUPPORTED_RATES = {0.25, 0.5, 1.0, 2.0, 3.0, 4.0};
    private static final int[] VELOCITY_CONTOUR = {18, -9, 12, -16, 14, -7, 9, -13, 16, -11, 10, -8, 13, -15, 15, -6};

    public NestedRhythmPattern generate(final Settings settings) {
        final Settings normalized = settings.normalized();
        final int barFineSteps = fineStepsPerBar(normalized.meterNumerator(), normalized.meterDenominator());
        final int totalFineSteps = barFineSteps * normalized.barCount();
        final TreeMap<Integer, PulseSpec> structure = fullStructure(normalized, barFineSteps, totalFineSteps);
        final List<PulseSpec> retained = thinStructure(structure, normalized.density(),
                normalized.cluster(), totalFineSteps);
        final List<Integer> durationStarts = List.copyOf(structure.keySet());
        final Map<Integer, Integer> durationCaps = durationCapsByOriginalStart(
                List.copyOf(structure.values()), durationStarts, normalized.cluster(), totalFineSteps);

        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(retained.size());
        final List<Integer> rawVelocities = new ArrayList<>(events.size());
        int lastClusteredStart = -1;
        for (int order = 0; order < retained.size(); order++) {
            final PulseSpec pulse = retained.get(order);
            final int structureOrder = durationStarts.indexOf(pulse.fineStart());
            final int clusteredStart = clusteredFineStart(pulse.fineStart(), order, retained.size(),
                    normalized.cluster(), totalFineSteps, lastClusteredStart, pulse.role());
            lastClusteredStart = clusteredStart;
            final int fineStart = clusteredStart;
            rawVelocities.add(velocityFor(pulse, structureOrder));
            events.add(new NestedRhythmPattern.PulseEvent(
                    structureOrder,
                    fineStart,
                    durationCaps.getOrDefault(pulse.fineStart(),
                            durationFor(pulse.fineStart(), pulse.role(), durationStarts, totalFineSteps)),
                    normalized.midiNote(),
                    0,
                    pulse.role()));
        }

        final List<Integer> shapedVelocities = applyVelocityDepth(rawVelocities,
                normalized.velocityDepth(), normalized.velocityCenter());
        final List<Integer> rotatedVelocities = rotateVelocities(shapedVelocities, normalized.velocityRotation());
        for (int index = 0; index < events.size(); index++) {
            final NestedRhythmPattern.PulseEvent event = events.get(index);
            events.set(index, new NestedRhythmPattern.PulseEvent(
                    event.order(),
                    event.fineStart(),
                    event.duration(),
                    event.midiNote(),
                    rotatedVelocities.get(index),
                    event.role()));
        }
        events.sort(Comparator.comparingInt(NestedRhythmPattern.PulseEvent::fineStart));
        final List<Integer> durationReferenceStarts = normalized.cluster() > 0.0001
                ? events.stream().map(NestedRhythmPattern.PulseEvent::fineStart).toList()
                : durationStarts;
        return new NestedRhythmPattern(withDurations(events, durationReferenceStarts, totalFineSteps));
    }

    private int clusteredFineStart(final int originalFineStart,
                                   final int retainedIndex,
                                   final int retainedCount,
                                   final double cluster,
                                   final int totalFineSteps,
                                   final int previousStart,
                                   final NestedRhythmPattern.Role role) {
        if (cluster <= 0.0001 || retainedCount <= 0 || totalFineSteps <= 1) {
            return originalFineStart;
        }
        final int windowLength = clusterWindowLength(cluster, totalFineSteps);
        final int windowStart = Math.max(0, totalFineSteps - windowLength);
        final double originalPosition = originalFineStart / (double) totalFineSteps;
        final int target = windowStart + (int) Math.round(originalPosition * Math.max(0, windowLength - 1));
        final int blended = (int) Math.round(originalFineStart * (1.0 - cluster) + target * cluster);
        final int constrained = Math.max(windowStart, blended);
        final int remaining = retainedCount - retainedIndex - 1;
        final int minStart = previousStart + 1;
        final int maxStart = Math.max(minStart, totalFineSteps - remaining - 1);
        return snapClusteredFineStart(constrained, role, retainedCount, cluster, minStart, maxStart,
                remaining, totalFineSteps);
    }

    private int snapClusteredFineStart(final int desiredFineStart,
                                       final NestedRhythmPattern.Role role,
                                       final int retainedCount,
                                       final double cluster,
                                       final int minStart,
                                       final int maxStart,
                                       final int remaining,
                                       final int totalFineSteps) {
        final int divisions = clusterGridDivisions(role, retainedCount, cluster, totalFineSteps);
        final int windowLength = clusterWindowLength(cluster, totalFineSteps);
        final int windowStart = cluster <= 0.0001 ? 0 : Math.max(0, totalFineSteps - windowLength);
        final int candidate = nearestGridStart(desiredFineStart, divisions, Math.max(minStart, windowStart),
                remaining, totalFineSteps);
        if (candidate >= 0) {
            return candidate;
        }
        return Math.max(minStart, Math.min(maxStart, desiredFineStart));
    }

    private int nearestGridStart(final int desiredFineStart,
                                 final int divisions,
                                 final int minStart,
                                 final int remaining,
                                 final int totalFineSteps) {
        final int minIndex = Math.max(0, (int) Math.floor(
                minStart * divisions / (double) FINE_STEPS_PER_WHOLE) - 2);
        final int maxIndex = (int) Math.ceil((totalFineSteps - 1) * divisions
                / (double) FINE_STEPS_PER_WHOLE) + 2;
        final List<Integer> candidates = new ArrayList<>();
        for (int index = minIndex; index <= maxIndex; index++) {
            final int candidate = (int) Math.round(index * FINE_STEPS_PER_WHOLE / (double) divisions);
            if (candidate < minStart || candidate >= totalFineSteps) {
                continue;
            }
            candidates.add(candidate);
        }
        if (candidates.isEmpty() || candidates.size() <= remaining) {
            return -1;
        }
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        final int maxCandidateIndex = candidates.size() - remaining - 1;
        for (int index = 0; index <= maxCandidateIndex; index++) {
            final int distance = Math.abs(candidates.get(index) - desiredFineStart);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        final int candidate = candidates.get(bestIndex);
        return candidate;
    }

    private int clusterGridDivisions(final NestedRhythmPattern.Role role,
                                     final int retainedCount,
                                     final double cluster,
                                     final int totalFineSteps) {
        if (isAnchor(role)) {
            return 16;
        }
        final int windowLength = clusterWindowLength(cluster, totalFineSteps);
        final int windowStart = Math.max(0, totalFineSteps - windowLength);
        return availableGridSlots(16, windowStart, totalFineSteps) >= retainedCount ? 16 : 32;
    }

    private int availableGridSlots(final int divisions,
                                   final int startInclusive,
                                   final int endExclusive) {
        int count = 0;
        final int minIndex = Math.max(0, (int) Math.ceil(
                startInclusive * divisions / (double) FINE_STEPS_PER_WHOLE));
        final int maxIndex = (int) Math.floor(
                (endExclusive - 1) * divisions / (double) FINE_STEPS_PER_WHOLE);
        for (int index = minIndex; index <= maxIndex; index++) {
            final int candidate = (int) Math.round(index * FINE_STEPS_PER_WHOLE / (double) divisions);
            if (candidate >= startInclusive && candidate < endExclusive) {
                count++;
            }
        }
        return count;
    }

    private boolean isAnchor(final NestedRhythmPattern.Role role) {
        return role == NestedRhythmPattern.Role.PRIMARY_ANCHOR
                || role == NestedRhythmPattern.Role.SECONDARY_ANCHOR;
    }

    private Map<Integer, Integer> durationCapsByOriginalStart(final List<PulseSpec> structurePulses,
                                                              final List<Integer> durationStarts,
                                                              final double cluster,
                                                              final int totalFineSteps) {
        final Map<Integer, Integer> caps = new HashMap<>();
        if (cluster <= 0.0001) {
            for (final PulseSpec pulse : structurePulses) {
                caps.put(pulse.fineStart(),
                        durationFor(pulse.fineStart(), pulse.role(), durationStarts, totalFineSteps));
            }
            return caps;
        }

        final Map<Integer, Integer> clusteredStartByOriginalStart = new HashMap<>();
        final List<Integer> clusteredStarts = new ArrayList<>(structurePulses.size());
        int lastClusteredStart = -1;
        for (int index = 0; index < structurePulses.size(); index++) {
            final PulseSpec pulse = structurePulses.get(index);
            final int rawClusteredStart = clusteredFineStart(pulse.fineStart(), index, structurePulses.size(),
                    cluster, totalFineSteps, lastClusteredStart, pulse.role());
            lastClusteredStart = rawClusteredStart;
            clusteredStartByOriginalStart.put(pulse.fineStart(), rawClusteredStart);
            clusteredStarts.add(rawClusteredStart);
        }
        clusteredStarts.sort(Comparator.naturalOrder());

        for (final PulseSpec pulse : structurePulses) {
            final int originalCap = durationFor(pulse.fineStart(), pulse.role(), durationStarts, totalFineSteps);
            final int clusteredCap = durationFor(clusteredStartByOriginalStart.get(pulse.fineStart()),
                    pulse.role(), clusteredStarts, totalFineSteps);
            caps.put(pulse.fineStart(), Math.min(originalCap, clusteredCap));
        }
        return caps;
    }

    public static int fineStepsPerBeat(final int denominator) {
        return FINE_STEPS_PER_WHOLE / normalizeDenominator(denominator);
    }

    public static int fineStepsPerBar(final int numerator, final int denominator) {
        return Math.max(1, normalizeNumerator(numerator) * fineStepsPerBeat(denominator));
    }

    public static double beatsPerBar(final int numerator, final int denominator) {
        return normalizeNumerator(numerator) * (4.0 / normalizeDenominator(denominator));
    }

    public static int maxFineStepsFor(final int maxBars, final int maxNumerator, final int minDenominator) {
        return Math.max(1, maxBars) * fineStepsPerBar(maxNumerator, minDenominator);
    }

    public static int[] supportedTupletCounts(final int meterNumerator,
                                              final int meterDenominator,
                                              final int tupletTargets) {
        final int barFineSteps = fineStepsPerBar(meterNumerator, meterDenominator);
        final int halfBarFineSteps = Math.max(1, barFineSteps / 2);
        final int spanFineSteps = halfBarFineSteps;
        final int eighthFineSteps = FINE_STEPS_PER_WHOLE / 8;
        final int spanUnits = Math.max(1, (int) Math.round(spanFineSteps / (double) eighthFineSteps));
        final List<Integer> supported = new ArrayList<>();
        supported.add(0);
        for (final int candidate : TUPLET_COUNT_CANDIDATES) {
            if (candidate == 0 || candidate == spanUnits) {
                continue;
            }
            if (greatestCommonDivisor(candidate, spanUnits) == 1) {
                supported.add(candidate);
            }
        }
        return supported.stream().mapToInt(Integer::intValue).toArray();
    }

    static int[] supportedTupletDivisions(final int meterNumerator,
                                          final int meterDenominator) {
        final int[] supportedCounts = supportedTupletCounts(meterNumerator, meterDenominator, 1);
        final List<Integer> divisions = new ArrayList<>();
        for (final int value : supportedCounts) {
            if (value > 0) {
                divisions.add(value);
            }
        }
        return divisions.stream().mapToInt(Integer::intValue).toArray();
    }

    public static int contourLength() {
        return VELOCITY_CONTOUR.length;
    }

    public static int contourAt(final int order) {
        return VELOCITY_CONTOUR[Math.floorMod(order, VELOCITY_CONTOUR.length)];
    }

    public static double[] supportedRates() {
        return SUPPORTED_RATES.clone();
    }

    private TreeMap<Integer, PulseSpec> fullStructure(final Settings settings,
                                                      final int barFineSteps,
                                                      final int totalFineSteps) {
        final TreeMap<Integer, PulseSpec> structure = new TreeMap<>();
        addBaseGrid(structure, settings, totalFineSteps);
        applyTuplet(structure, settings, barFineSteps, totalFineSteps);
        applyRatchet(structure, settings, barFineSteps, totalFineSteps);
        return structure;
    }

    private void addBaseGrid(final TreeMap<Integer, PulseSpec> structure,
                             final Settings settings,
                             final int totalFineSteps) {
        final int beatFineSteps = fineStepsPerBeat(settings.meterDenominator());
        final List<Integer> starts = baseGridStarts(beatFineSteps, totalFineSteps, settings.rate());
        int contourIndex = 0;
        for (final int fineStart : starts) {
            final boolean beatAligned = isGridAligned(fineStart, beatFineSteps);
            final NestedRhythmPattern.Role role = beatAligned
                    ? NestedRhythmPattern.Role.PRIMARY_ANCHOR
                    : NestedRhythmPattern.Role.SECONDARY_ANCHOR;
            structure.put(fineStart, new PulseSpec(
                    fineStart,
                    role,
                    GestureFamily.ANCHOR,
                    beatAligned,
                    beatAligned ? 1000 : rolePriority(role) - contourIndex,
                    contourIndex,
                    starts.size(),
                    contourIndex,
                    starts.size()));
            contourIndex++;
        }
    }

    private static List<Integer> baseGridStarts(final int beatFineSteps,
                                                final int totalFineSteps,
                                                final double rate) {
        final LinkedHashSet<Integer> starts = new LinkedHashSet<>();
        final double step = Math.max(1.0, beatFineSteps / Math.max(0.0001, rate));
        for (double position = 0.0; position < totalFineSteps - 0.0001; position += step) {
            starts.add(Math.max(0, Math.min(totalFineSteps - 1, (int) Math.round(position))));
        }
        return List.copyOf(starts);
    }

    private static boolean isGridAligned(final int fineStart,
                                         final int gridSize) {
        if (gridSize <= 1) {
            return true;
        }
        final int remainder = Math.floorMod(fineStart, gridSize);
        return remainder == 0 || remainder == gridSize - 1;
    }

    private void applyTuplet(final TreeMap<Integer, PulseSpec> structure,
                             final Settings settings,
                             final int barFineSteps,
                             final int totalFineSteps) {
        if (settings.tupletDivisions() == 0 || settings.tupletTargets() == 0) {
            return;
        }
        final int spanFineSteps = tupletSpanFineSteps(barFineSteps, settings.rate());
        final int totalSpans = totalTupletSpans(totalFineSteps, spanFineSteps);
        final List<Integer> selectedSpans = selectedTupletTargetIndices(
                totalSpans, settings.tupletTargets(), settings.tupletTargetPhase());
        final int contourCount = Math.max(1, selectedSpans.size() * settings.tupletDivisions());
        int contourOffset = 0;
        for (final int spanIndex : selectedSpans) {
            final int start = Math.floorMod(spanIndex * spanFineSteps, totalFineSteps);
            if (settings.cluster() <= 0.0001) {
                clearWrappedSpan(structure, start, spanFineSteps, totalFineSteps);
            }
            final List<Integer> starts = evenlyDividedWrappedStarts(
                    start, spanFineSteps, settings.tupletDivisions(), totalFineSteps);
            addSubdivisionPulses(structure, starts, settings.tupletDivisions(), contourOffset, contourCount,
                    NestedRhythmPattern.Role.TUPLET_LEAD, NestedRhythmPattern.Role.TUPLET_INTERIOR,
                    GestureFamily.TUPLET);
            contourOffset += settings.tupletDivisions();
        }
    }

    private static int tupletSpanFineSteps(final int barFineSteps,
                                           final double rate) {
        return Math.max(1, (int) Math.round((barFineSteps / 2.0) / Math.max(0.0001, rate)));
    }

    private static int totalTupletSpans(final int totalFineSteps,
                                        final int spanFineSteps) {
        return Math.max(1, (int) Math.round(totalFineSteps / (double) Math.max(1, spanFineSteps)));
    }

    private void applyRatchet(final TreeMap<Integer, PulseSpec> structure,
                              final Settings settings,
                              final int barFineSteps,
                              final int totalFineSteps) {
        if (settings.ratchetDivisions() == 0 || settings.ratchetTargets() == 0) {
            return;
        }
        final List<RatchetTargetRegion> targetRegions = ratchetTargetRegions(structure, settings, totalFineSteps);
        final int contourCount = Math.max(1, targetRegions.size() * settings.ratchetDivisions());
        int contourOffset = 0;
        for (final RatchetTargetRegion target : targetRegions) {
            clearWrappedSpan(structure, target.start(), target.length(), totalFineSteps);
            final List<Integer> starts = evenlyDividedWrappedStarts(
                    target.start(), target.length(), settings.ratchetDivisions(), totalFineSteps);
            addSubdivisionPulses(structure, starts, settings.ratchetDivisions(), contourOffset, contourCount,
                    NestedRhythmPattern.Role.RATCHET_LEAD, NestedRhythmPattern.Role.RATCHET_INTERIOR,
                    target.gestureFamily());
            contourOffset += settings.ratchetDivisions();
        }
    }

    private void addSubdivisionPulses(final TreeMap<Integer, PulseSpec> structure,
                                      final List<Integer> starts,
                                      final int count,
                                      final NestedRhythmPattern.Role leadRole,
                                      final NestedRhythmPattern.Role interiorRole,
                                      final GestureFamily gestureFamily) {
        int inserted = 0;
        for (int index = 0; index < starts.size(); index++) {
            final int fineStart = starts.get(index);
            if (structure.containsKey(fineStart)) {
                continue;
            }
            final NestedRhythmPattern.Role role = inserted == 0 ? leadRole : interiorRole;
            structure.put(fineStart, new PulseSpec(
                    fineStart,
                    role,
                    gestureFamily,
                        false,
                        subdivisionPriority(role, index, count),
                        index,
                        count,
                        index,
                        count));
            inserted++;
        }
    }

    private void addSubdivisionPulses(final TreeMap<Integer, PulseSpec> structure,
                                      final List<Integer> starts,
                                      final int localCount,
                                      final int contourOffset,
                                      final int contourCount,
                                      final NestedRhythmPattern.Role leadRole,
                                      final NestedRhythmPattern.Role interiorRole,
                                      final GestureFamily gestureFamily) {
        int inserted = 0;
        for (int index = 0; index < starts.size(); index++) {
            final int fineStart = starts.get(index);
            if (structure.containsKey(fineStart)) {
                continue;
            }
            final NestedRhythmPattern.Role role = inserted == 0 ? leadRole : interiorRole;
            structure.put(fineStart, new PulseSpec(
                    fineStart,
                    role,
                    gestureFamily,
                    false,
                    subdivisionPriority(role, index, localCount),
                    index,
                    localCount,
                    contourOffset + index,
                    contourCount));
            inserted++;
        }
    }

    private List<PulseSpec> thinStructure(final TreeMap<Integer, PulseSpec> structure,
                                          final double density,
                                          final double cluster,
                                          final int totalFineSteps) {
        final List<PulseSpec> required = new ArrayList<>();
        final List<PulseSpec> optional = new ArrayList<>();
        for (final PulseSpec pulse : structure.values()) {
            if (pulse.required()) {
                required.add(pulse);
            } else {
                optional.add(pulse);
            }
        }
        if (optional.isEmpty()) {
            return required;
        }
        if (optional.stream().allMatch(pulse -> pulse.gestureFamily() == GestureFamily.ANCHOR)) {
            final int keepCount = optionalKeepCount(optional.size(), density);
            final List<PulseSpec> retained = new ArrayList<>(required);
            retained.addAll(selectOptionalPulses(optional, keepCount, cluster, totalFineSteps));
            retained.sort(Comparator.comparingInt(PulseSpec::fineStart));
            return retained;
        }
        final int keepCount = identityKeepCount(structure.values(), density);
        final List<PulseSpec> candidates = new ArrayList<>(structure.values());
        if (cluster > 0.0001) {
            return selectOptionalPulses(candidates, keepCount, cluster, totalFineSteps);
        }
        return selectOptionalPulses(candidates, keepCount, cluster, totalFineSteps);
    }

    private int optionalKeepCount(final int candidateCount,
                                  final double density) {
        return Math.max(0, Math.min(candidateCount, (int) Math.round(density * candidateCount)));
    }

    private int identityKeepCount(final Iterable<PulseSpec> candidates,
                                  final double density) {
        final EnumMap<GestureFamily, Boolean> generatedFamilies = new EnumMap<>(GestureFamily.class);
        int candidateCount = 0;
        for (final PulseSpec pulse : candidates) {
            candidateCount++;
            if (pulse.gestureFamily() != GestureFamily.ANCHOR) {
                generatedFamilies.put(pulse.gestureFamily(), true);
            }
        }
        if (candidateCount <= 0) {
            return 0;
        }
        final double phraseDensity = GENERATED_DENSITY_FLOOR + density * (1.0 - GENERATED_DENSITY_FLOOR);
        final int densityCount = optionalKeepCount(candidateCount, phraseDensity);
        final int phraseCellFloor = Math.max(2, generatedFamilies.size() * 2);
        return Math.max(densityCount, Math.min(candidateCount, phraseCellFloor));
    }

    private List<PulseSpec> selectOptionalPulses(final List<PulseSpec> optional,
                                                 final int keepCount,
                                                 final double cluster,
                                                 final int totalFineSteps) {
        if (keepCount <= 0 || optional.isEmpty()) {
            return List.of();
        }
        final List<PulseSpec> ranked = rankedOptionalPulses(optional, cluster, totalFineSteps);
        final List<PulseSpec> selected = new ArrayList<>(ranked.subList(0, Math.min(keepCount, ranked.size())));
        ensureGeneratedFamilyCoverage(selected, ranked, keepCount);
        selected.sort(Comparator.comparingInt(PulseSpec::fineStart));
        return selected;
    }

    private void ensureGeneratedFamilyCoverage(final List<PulseSpec> selected,
                                               final List<PulseSpec> ranked,
                                               final int keepCount) {
        final Set<GestureFamily> generatedFamilies = ranked.stream()
                .map(PulseSpec::gestureFamily)
                .filter(family -> family != GestureFamily.ANCHOR)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(GestureFamily.class)));
        if (selected.size() < keepCount || keepCount < generatedFamilies.size() || generatedFamilies.size() <= 1) {
            return;
        }
        for (final GestureFamily family : generatedFamilies) {
            if (selected.stream().anyMatch(pulse -> pulse.gestureFamily() == family)) {
                continue;
            }
            final PulseSpec replacement = ranked.stream()
                    .filter(pulse -> pulse.gestureFamily() == family)
                    .findFirst()
                    .orElse(null);
            final int replaceIndex = leastEssentialSelectedIndex(selected);
            if (replacement != null && replaceIndex >= 0) {
                selected.set(replaceIndex, replacement);
            }
        }
    }

    private int leastEssentialSelectedIndex(final List<PulseSpec> selected) {
        final Map<GestureFamily, Long> counts = selected.stream()
                .collect(Collectors.groupingBy(PulseSpec::gestureFamily, () -> new EnumMap<>(GestureFamily.class),
                        Collectors.counting()));
        int bestIndex = -1;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int index = 0; index < selected.size(); index++) {
            final PulseSpec pulse = selected.get(index);
            final long familyCount = counts.getOrDefault(pulse.gestureFamily(), 0L);
            final double score = (pulse.gestureFamily() == GestureFamily.ANCHOR ? 0.0 : 1000.0)
                    + (familyCount > 1 ? 0.0 : 500.0)
                    + pulse.priority();
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private List<PulseSpec> rankedOptionalPulses(final List<PulseSpec> optional,
                                                 final double cluster,
                                                 final int totalFineSteps) {
        final List<PulseSpec> timeOrdered = new ArrayList<>(optional);
        timeOrdered.sort(Comparator.comparingInt(PulseSpec::fineStart));
        final List<PulseSpec> ranked = new ArrayList<>(optional.size());
        final Set<Integer> selectedStarts = new HashSet<>();
        final Map<GestureFamily, Integer> selectedFamilyCounts = new EnumMap<>(GestureFamily.class);

        while (ranked.size() + 1 < optional.size()) {
            final PulsePair pair = bestOptionalPair(
                    timeOrdered, selectedStarts, selectedFamilyCounts, cluster, totalFineSteps);
            if (pair == null) {
                break;
            }
            ranked.add(pair.left());
            ranked.add(pair.right());
            markSelected(pair.left(), selectedStarts, selectedFamilyCounts);
            markSelected(pair.right(), selectedStarts, selectedFamilyCounts);
        }

        while (ranked.size() < optional.size()) {
            final PulseSpec pulse = bestOptionalSingle(
                    timeOrdered, selectedStarts, selectedFamilyCounts, cluster, totalFineSteps);
            if (pulse == null) {
                break;
            }
            ranked.add(pulse);
            markSelected(pulse, selectedStarts, selectedFamilyCounts);
        }
        return ranked;
    }

    private PulsePair bestOptionalPair(final List<PulseSpec> optional,
                                       final Set<Integer> selectedStarts,
                                       final Map<GestureFamily, Integer> selectedFamilyCounts,
                                       final double cluster,
                                       final int totalFineSteps) {
        PulsePair best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int index = 0; index + 1 < optional.size(); index++) {
            final PulseSpec left = optional.get(index);
            final PulseSpec right = optional.get(index + 1);
            if (selectedStarts.contains(left.fineStart()) || selectedStarts.contains(right.fineStart())) {
                continue;
            }
            if (!sameGestureFamily(left, right)) {
                continue;
            }
            final double score = optionalScore(left, cluster, totalFineSteps)
                    + optionalScore(right, cluster, totalFineSteps)
                    + phraseGroupScore(left.gestureFamily(), selectedFamilyCounts)
                    + compactPairBonus(left, right, totalFineSteps)
                    + localGestureIdentityBonus(left, right);
            if (score > bestScore) {
                bestScore = score;
                best = new PulsePair(left, right);
            }
        }
        return best;
    }

    private boolean sameGestureFamily(final PulseSpec left,
                                      final PulseSpec right) {
        return left.gestureFamily() == right.gestureFamily();
    }

    private PulseSpec bestOptionalSingle(final List<PulseSpec> optional,
                                         final Set<Integer> selectedStarts,
                                         final Map<GestureFamily, Integer> selectedFamilyCounts,
                                         final double cluster,
                                         final int totalFineSteps) {
        PulseSpec best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (final PulseSpec pulse : optional) {
            if (selectedStarts.contains(pulse.fineStart())) {
                continue;
            }
            final double score = optionalScore(pulse, cluster, totalFineSteps)
                    + singleGestureScore(pulse.gestureFamily(), selectedFamilyCounts);
            if (score > bestScore) {
                bestScore = score;
                best = pulse;
            }
        }
        return best;
    }

    private void markSelected(final PulseSpec pulse,
                              final Set<Integer> selectedStarts,
                              final Map<GestureFamily, Integer> selectedFamilyCounts) {
        selectedStarts.add(pulse.fineStart());
        selectedFamilyCounts.merge(pulse.gestureFamily(), 1, Integer::sum);
    }

    private double optionalScore(final PulseSpec pulse,
                                 final double cluster,
                                 final int totalFineSteps) {
        return selectionPriority(pulse)
                + phrasePositionScore(pulse.fineStart(), totalFineSteps) * 90.0
                + cluster * clusterMembership(pulse.fineStart(), cluster, totalFineSteps) * 900.0;
    }

    private double selectionPriority(final PulseSpec pulse) {
        if (pulse.gestureFamily() == GestureFamily.ANCHOR) {
            return pulse.required() ? 48.0 : 36.0;
        }
        return pulse.priority();
    }

    private double phraseGroupScore(final GestureFamily family,
                                    final Map<GestureFamily, Integer> selectedFamilyCounts) {
        final int selectedCount = selectedFamilyCounts.getOrDefault(family, 0);
        final double novelty = switch (family) {
            case NESTED_RATCHET -> 170.0;
            case TUPLET -> 145.0;
            case RATCHET -> 85.0;
            case ANCHOR -> 60.0;
            case OTHER -> 40.0;
        };
        return novelty - selectedCount * 70.0;
    }

    private double singleGestureScore(final GestureFamily family,
                                      final Map<GestureFamily, Integer> selectedFamilyCounts) {
        final int selectedCount = selectedFamilyCounts.getOrDefault(family, 0);
        final double novelty = switch (family) {
            case TUPLET -> 80.0;
            case NESTED_RATCHET -> 70.0;
            case RATCHET -> 35.0;
            case ANCHOR -> 30.0;
            case OTHER -> 20.0;
        };
        return novelty - selectedCount * 35.0;
    }

    private double compactPairBonus(final PulseSpec left,
                                    final PulseSpec right,
                                    final int totalFineSteps) {
        final int distance = Math.max(1, right.fineStart() - left.fineStart());
        final double compactness = 1.0 - Math.min(1.0, distance / (double) Math.max(1, totalFineSteps));
        return 75.0 + compactness * 24.0;
    }

    private double localGestureIdentityBonus(final PulseSpec left,
                                             final PulseSpec right) {
        if (!sameGestureFamily(left, right) || left.subdivisionCount() <= 1 || right.subdivisionCount() <= 1) {
            return 0.0;
        }
        double bonus = 0.0;
        if (sameLocalSubdivisionCell(left, right)) {
            bonus += 180.0;
        }
        if (Math.abs(left.subdivisionIndex() - right.subdivisionIndex()) == 1) {
            bonus += 70.0;
        }
        if (left.subdivisionIndex() == 0 || right.subdivisionIndex() == 0) {
            bonus += 45.0;
        }
        if (left.subdivisionIndex() == left.subdivisionCount() - 1
                || right.subdivisionIndex() == right.subdivisionCount() - 1) {
            bonus += 25.0;
        }
        return bonus;
    }

    private boolean sameLocalSubdivisionCell(final PulseSpec left,
                                             final PulseSpec right) {
        if (left.subdivisionCount() != right.subdivisionCount()) {
            return false;
        }
        final int count = Math.max(1, left.subdivisionCount());
        return left.contourIndex() / count == right.contourIndex() / count;
    }

    private double phrasePositionScore(final int fineStart,
                                       final int totalFineSteps) {
        if (totalFineSteps <= 1) {
            return 1.0;
        }
        final double position = fineStart / (double) totalFineSteps;
        final double metric = metricPositionWeight(fineStart, totalFineSteps);
        final double call = gaussian(position, 0.25, 0.13);
        final double response = gaussian(position, 0.50, 0.15) * 0.84;
        final double cadence = gaussian(position, 0.88, 0.12) * 0.72;
        final double start = gaussian(position, 0.0, 0.10) * 0.62;
        return Math.max(metric, Math.max(Math.max(call, response), Math.max(cadence, start)));
    }

    private static double metricPositionWeight(final int fineStart,
                                               final int totalFineSteps) {
        if (fineStart <= 0) {
            return 1.0;
        }
        final int common = greatestCommonDivisor(fineStart, Math.max(1, totalFineSteps));
        return Math.log(common + 1.0) / Math.log(Math.max(2, totalFineSteps) + 1.0);
    }

    private static double gaussian(final double value,
                                   final double center,
                                   final double width) {
        final double distance = Math.abs(value - center);
        final double wrappedDistance = Math.min(distance, 1.0 - distance);
        final double normalized = wrappedDistance / Math.max(0.0001, width);
        return Math.exp(-0.5 * normalized * normalized);
    }

    private double clusterMembership(final int fineStart,
                                     final double cluster,
                                     final int totalFineSteps) {
        if (cluster <= 0.0001 || totalFineSteps <= 1) {
            return 0.0;
        }
        final int windowLength = clusterWindowLength(cluster, totalFineSteps);
        final int start = Math.max(0, totalFineSteps - windowLength);
        if (fineStart >= start) {
            return 1.0;
        }
        return fineStart / (double) Math.max(1, start);
    }

    private int clusterWindowLength(final double cluster, final int totalFineSteps) {
        final double windowStartFraction = Math.min(0.75, Math.max(0.0, cluster));
        return Math.max(1, totalFineSteps - (int) Math.round(totalFineSteps * windowStartFraction));
    }

    private List<RatchetTargetRegion> ratchetTargetRegions(final TreeMap<Integer, PulseSpec> structure,
                                                           final Settings settings,
                                                           final int totalFineSteps) {
        final List<RatchetTargetRegion> regions = ratchetParentRegions(structure, totalFineSteps);
        if (regions.isEmpty()) {
            return List.of();
        }
        final List<RatchetTargetRegion> ordered = ratchetTargetPriorityOrder(regions, settings.barCount(), settings.rate());
        final int targetCount = Math.max(0, Math.min(ordered.size(), settings.ratchetTargets()));
        final int targetPhase = Math.floorMod(settings.ratchetTargetPhase(), ordered.size());
        final List<RatchetTargetRegion> selected = new ArrayList<>(targetCount);
        for (int index = 0; index < targetCount; index++) {
            selected.add(ordered.get(Math.floorMod(index + targetPhase, ordered.size())));
        }
        selected.sort(Comparator.comparingInt(RatchetTargetRegion::start));
        return selected;
    }

    private List<RatchetTargetRegion> ratchetParentRegions(final TreeMap<Integer, PulseSpec> structure,
                                                           final int totalFineSteps) {
        if (structure.isEmpty() || totalFineSteps <= 0) {
            return List.of();
        }
        final List<PulseSpec> pulses = new ArrayList<>(structure.values());
        pulses.sort(Comparator.comparingInt(PulseSpec::fineStart));
        final List<RatchetTargetRegion> regions = new ArrayList<>(pulses.size());
        for (int index = 0; index < pulses.size(); index++) {
            final PulseSpec pulse = pulses.get(index);
            final PulseSpec next = pulses.get((index + 1) % pulses.size());
            final int length = next.fineStart() > pulse.fineStart()
                    ? next.fineStart() - pulse.fineStart()
                    : totalFineSteps - pulse.fineStart() + next.fineStart();
            regions.add(new RatchetTargetRegion(pulse.fineStart(), Math.max(1, length),
                    ratchetGestureFamilyFor(pulse)));
        }
        return regions;
    }

    private GestureFamily ratchetGestureFamilyFor(final PulseSpec parent) {
        if (parent.gestureFamily() == GestureFamily.TUPLET) {
            return GestureFamily.NESTED_RATCHET;
        }
        return GestureFamily.RATCHET;
    }

    private List<RatchetTargetRegion> ratchetTargetPriorityOrder(final List<RatchetTargetRegion> regions,
                                                                 final int barCount,
                                                                 final double rate) {
        if (regions.stream().anyMatch(region -> region.gestureFamily() == GestureFamily.NESTED_RATCHET)) {
            final List<RatchetTargetRegion> ordered = new ArrayList<>(regions.size());
            for (final int index : complementaryRatchetTargetPriorityOrder(regions)) {
                ordered.add(regions.get(index));
            }
            return ordered;
        }
        if (barCount <= 1) {
            return Math.abs(rate - 1.0) <= 0.0001
                    ? regionPriorityOrder(regions, false)
                    : regionPriorityOrder(regions, true);
        }
        final List<RatchetTargetRegion> ordered = new ArrayList<>(regions.size());
        for (final int index : phraseTargetPriorityOrder(regions.size())) {
            ordered.add(regions.get(index));
        }
        return ordered;
    }

    private List<RatchetTargetRegion> regionPriorityOrder(final List<RatchetTargetRegion> regions,
                                                          final boolean phrasePositioned) {
        if (regions.isEmpty()) {
            return List.of();
        }
        final List<RatchetTargetRegion> ordered = new ArrayList<>(regions.size());
        final List<Integer> priorityOrder = phrasePositioned
                ? structuralTargetPriorityOrder(regions.size())
                : targetPriorityOrder(regions.size());
        for (final int index : priorityOrder) {
            ordered.add(regions.get(index));
        }
        return ordered;
    }

    static List<Integer> targetPriorityOrder(final int targetCount) {
        if (targetCount <= 0) {
            return List.of();
        }
        final List<Integer> ordered = new ArrayList<>(targetCount);
        addTargetIndex(ordered, targetCount, 1);
        addTargetIndex(ordered, targetCount, 0);
        addTargetIndex(ordered, targetCount, targetCount - 1);
        for (int offset = 2; ordered.size() < targetCount; offset++) {
            addTargetIndex(ordered, targetCount, offset);
            addTargetIndex(ordered, targetCount, targetCount - offset);
        }
        return ordered;
    }

    static List<Integer> tupletTargetPriorityOrder(final int targetCount) {
        return scoredTargetPriorityOrder(targetCount, TargetOrderProfile.TUPLET);
    }

    private static List<Integer> structuralTargetPriorityOrder(final int targetCount) {
        return scoredTargetPriorityOrder(targetCount, TargetOrderProfile.STRUCTURAL);
    }

    private static List<Integer> phraseTargetPriorityOrder(final int targetCount) {
        return scoredTargetPriorityOrder(targetCount, TargetOrderProfile.RATCHET);
    }

    private static List<Integer> complementaryRatchetTargetPriorityOrder(final List<RatchetTargetRegion> regions) {
        final int targetCount = regions.size();
        if (targetCount <= 0) {
            return List.of();
        }
        final List<Integer> ordered = new ArrayList<>(targetCount);
        addComplementaryRatchetSeedTargets(ordered, regions);
        final List<Integer> indices = new ArrayList<>(targetCount);
        for (int index = 0; index < targetCount; index++) {
            if (!ordered.contains(index)) {
                indices.add(index);
            }
        }
        final List<Integer> stableTieBreak = targetPriorityOrder(targetCount);
        indices.sort(Comparator
                .comparingDouble((Integer index) -> complementaryRatchetTargetScore(index, regions))
                .reversed()
                .thenComparingInt(stableTieBreak::indexOf));
        ordered.addAll(indices);
        return ordered;
    }

    private static void addComplementaryRatchetSeedTargets(final List<Integer> ordered,
                                                           final List<RatchetTargetRegion> regions) {
        addTargetIndex(ordered, regions.size(), 0);
        final int firstNested = firstRegionWithFamily(regions, GestureFamily.NESTED_RATCHET);
        addTargetIndex(ordered, regions.size(), firstNested);
        final int lastNested = lastRegionWithFamily(regions, GestureFamily.NESTED_RATCHET);
        addTargetIndex(ordered, regions.size(), lastNested);
    }

    private static double complementaryRatchetTargetScore(final int index,
                                                          final List<RatchetTargetRegion> regions) {
        final RatchetTargetRegion region = regions.get(index);
        final double familyBias = region.gestureFamily() == GestureFamily.NESTED_RATCHET ? -0.04 : 0.08;
        return targetPhraseScore(index, regions.size(), TargetOrderProfile.RATCHET) + familyBias;
    }

    private static int firstRegionWithFamily(final List<RatchetTargetRegion> regions,
                                             final GestureFamily family) {
        for (int index = 0; index < regions.size(); index++) {
            if (regions.get(index).gestureFamily() == family) {
                return index;
            }
        }
        return -1;
    }

    private static int lastRegionWithFamily(final List<RatchetTargetRegion> regions,
                                            final GestureFamily family) {
        for (int index = regions.size() - 1; index >= 0; index--) {
            if (regions.get(index).gestureFamily() == family) {
                return index;
            }
        }
        return -1;
    }

    private static List<Integer> scoredTargetPriorityOrder(final int targetCount,
                                                           final TargetOrderProfile profile) {
        if (targetCount <= 0) {
            return List.of();
        }
        final List<Integer> indices = new ArrayList<>(targetCount);
        for (int index = 0; index < targetCount; index++) {
            indices.add(index);
        }
        final List<Integer> stableTieBreak = targetPriorityOrder(targetCount);
        indices.sort(Comparator
                .comparingDouble((Integer index) -> targetPhraseScore(index, targetCount, profile))
                .reversed()
                .thenComparingInt(stableTieBreak::indexOf));
        return indices;
    }

    private static double targetPhraseScore(final int index,
                                            final int targetCount,
                                            final TargetOrderProfile profile) {
        final double position = index / (double) Math.max(1, targetCount);
        final double metric = targetMetricWeight(index, targetCount);
        final double anticipation = targetMetricWeight(Math.floorMod(index + 1, targetCount), targetCount);
        final boolean compactPhrase = profile == TargetOrderProfile.STRUCTURAL;
        final double call = gaussian(position, 0.25, profile == TargetOrderProfile.TUPLET ? 0.16
                : compactPhrase ? 0.18 : 0.13);
        final double response = gaussian(position, 0.50, profile == TargetOrderProfile.TUPLET ? 0.18
                : compactPhrase ? 0.20 : 0.15);
        final double cadence = gaussian(position, 0.88, profile == TargetOrderProfile.TUPLET ? 0.14
                : compactPhrase ? 0.16 : 0.12);
        final double ground = gaussian(position, 0.0, profile == TargetOrderProfile.TUPLET ? 0.12
                : compactPhrase ? 0.13 : 0.10);
        final double responseWeight = profile == TargetOrderProfile.TUPLET ? 0.95 : compactPhrase ? 0.92 : 0.90;
        final double cadenceWeight = profile == TargetOrderProfile.TUPLET ? 0.58 : compactPhrase ? 0.82 : 0.72;
        final double groundWeight = profile == TargetOrderProfile.TUPLET ? 0.42 : compactPhrase ? 0.72 : 0.50;
        final double phraseRole = Math.max(Math.max(call, response * responseWeight),
                Math.max(cadence * cadenceWeight, ground * groundWeight));
        return profile == TargetOrderProfile.TUPLET
                ? metric * 0.28 + phraseRole * 0.58 + anticipation * 0.14
                : metric * 0.34 + phraseRole * 0.46 + anticipation * 0.20;
    }

    private static double targetMetricWeight(final int index,
                                             final int targetCount) {
        if (index == 0) {
            return 1.0;
        }
        final int common = greatestCommonDivisor(index, Math.max(1, targetCount));
        return Math.log(common + 1.0) / Math.log(Math.max(2, targetCount) + 1.0);
    }

    private static void addTargetIndex(final List<Integer> ordered,
                                       final int targetCount,
                                       final int index) {
        if (index >= 0 && index < targetCount && !ordered.contains(index)) {
            ordered.add(index);
        }
    }

    private static List<Integer> selectedTupletTargetIndices(final int totalTargets,
                                                             final int selectedCount,
                                                             final int targetPhase) {
        if (totalTargets <= 0 || selectedCount <= 0) {
            return List.of();
        }
        final List<Integer> ordered = tupletTargetPriorityOrder(totalTargets);
        final int count = Math.max(0, Math.min(totalTargets, selectedCount));
        final int phase = Math.floorMod(targetPhase, totalTargets);
        final List<Integer> selected = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            selected.add(ordered.get(Math.floorMod(index + phase, totalTargets)));
        }
        selected.sort(Comparator.naturalOrder());
        return selected;
    }

    static int ratchetParentRegionCount(final int meterNumerator,
                                        final int meterDenominator,
                                        final int barCount,
                                        final int tupletDivisions,
                                        final int tupletTargets,
                                        final int tupletTargetPhase,
                                        final double cluster) {
        return ratchetParentRegionCount(meterNumerator, meterDenominator, barCount, tupletDivisions,
                tupletTargets, tupletTargetPhase, cluster, 1.0);
    }

    static int ratchetParentRegionCount(final int meterNumerator,
                                        final int meterDenominator,
                                        final int barCount,
                                        final int tupletDivisions,
                                        final int tupletTargets,
                                        final int tupletTargetPhase,
                                        final double cluster,
                                        final double rate) {
        final int normalizedMeterNumerator = normalizeNumerator(meterNumerator);
        final int normalizedMeterDenominator = normalizeDenominator(meterDenominator);
        final int normalizedBarCount = Math.max(1, Math.min(MAX_BARS, barCount));
        final int barFineSteps = fineStepsPerBar(normalizedMeterNumerator, normalizedMeterDenominator);
        final int totalFineSteps = barFineSteps * normalizedBarCount;
        final TreeMap<Integer, Boolean> starts = new TreeMap<>();
        final int beatFineSteps = fineStepsPerBeat(normalizedMeterDenominator);
        final double normalizedRate = normalizeRate(rate);
        for (final int fineStart : baseGridStarts(beatFineSteps, totalFineSteps, normalizedRate)) {
            starts.put(fineStart, true);
        }
        if (tupletDivisions <= 0 || tupletTargets <= 0) {
            return starts.size();
        }
        final int spanFineSteps = tupletSpanFineSteps(barFineSteps, normalizedRate);
        final int totalSpans = totalTupletSpans(totalFineSteps, spanFineSteps);
        for (final int spanIndex : selectedTupletTargetIndices(totalSpans, tupletTargets, tupletTargetPhase)) {
            final int start = Math.floorMod(spanIndex * spanFineSteps, totalFineSteps);
            if (cluster <= 0.0001) {
                starts.keySet().removeIf(candidate ->
                        isInsideWrappedSpan(candidate, start, spanFineSteps, totalFineSteps));
            }
            for (final int tupletStart : wrappedSubdivisionStarts(
                    start, spanFineSteps, tupletDivisions, totalFineSteps)) {
                starts.put(tupletStart, true);
            }
        }
        return Math.max(1, starts.size());
    }

    private static boolean isInsideWrappedSpan(final int candidate,
                                               final int start,
                                               final int length,
                                               final int totalFineSteps) {
        if (length >= totalFineSteps) {
            return true;
        }
        final int end = start + length;
        if (end <= totalFineSteps) {
            return candidate >= start && candidate < end;
        }
        final int wrappedEnd = Math.floorMod(end, totalFineSteps);
        return candidate >= start || candidate < wrappedEnd;
    }

    private static List<Integer> wrappedSubdivisionStarts(final int start,
                                                          final int length,
                                                          final int count,
                                                          final int totalFineSteps) {
        final LinkedHashSet<Integer> starts = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            starts.add(Math.floorMod(start + index * (length / count), totalFineSteps));
        }
        return List.copyOf(starts);
    }

    private int subdivisionPriority(final NestedRhythmPattern.Role role, final int index, final int count) {
        final int base = rolePriority(role);
        if (index == 0) {
            return base;
        }
        final int mirroredDistance = Math.min(index, count - 1 - index);
        final int edgeBias = index == count - 1 ? 24 : 0;
        return base - mirroredDistance * 10 + edgeBias - index;
    }

    private int rolePriority(final NestedRhythmPattern.Role role) {
        return switch (role) {
            case PRIMARY_ANCHOR -> 1000;
            case RATCHET_LEAD -> 960;
            case TUPLET_LEAD -> 955;
            case SECONDARY_ANCHOR -> 940;
            case RATCHET_INTERIOR -> 920;
            case TUPLET_INTERIOR -> 915;
            case PICKUP -> 910;
        };
    }

    private List<Integer> evenlyDividedStarts(final int start, final int length, final int count) {
        final LinkedHashSet<Integer> starts = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            starts.add(start + index * (length / count));
        }
        return List.copyOf(starts);
    }

    private List<Integer> evenlyDividedWrappedStarts(final int start,
                                                     final int length,
                                                     final int count,
                                                     final int totalFineSteps) {
        final LinkedHashSet<Integer> starts = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            starts.add(Math.floorMod(start + index * (length / count), totalFineSteps));
        }
        return List.copyOf(starts);
    }

    private void clearWrappedSpan(final TreeMap<Integer, PulseSpec> structure,
                                  final int start,
                                  final int length,
                                  final int totalFineSteps) {
        if (length >= totalFineSteps) {
            structure.clear();
            return;
        }
        final int end = start + length;
        if (end <= totalFineSteps) {
            structure.subMap(start, true, end - 1, true).clear();
            return;
        }
        structure.subMap(start, true, totalFineSteps - 1, true).clear();
        final int wrappedEnd = Math.floorMod(end, totalFineSteps);
        if (wrappedEnd > 0) {
            structure.subMap(0, true, wrappedEnd - 1, true).clear();
        }
    }

    private List<NestedRhythmPattern.PulseEvent> withDurations(final List<NestedRhythmPattern.PulseEvent> startsOnly,
                                                               final List<Integer> durationStarts,
                                                               final int totalFineSteps) {
        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(startsOnly.size());
        for (final NestedRhythmPattern.PulseEvent current : startsOnly) {
            final int loopEndCap = Math.max(1, totalFineSteps - current.fineStart());
            final int duration = Math.min(loopEndCap, Math.min(
                    durationFor(current.fineStart(), current.role(), durationStarts, totalFineSteps),
                    Math.max(1, current.duration())));
            events.add(new NestedRhythmPattern.PulseEvent(current.order(), current.fineStart(),
                    duration, current.midiNote(), current.velocity(), current.role()));
        }
        return events;
    }

    private int durationFor(final int fineStart,
                            final NestedRhythmPattern.Role role,
                            final List<Integer> durationStarts,
                            final int totalFineSteps) {
        final int gap = gapToNextDurationStart(fineStart, durationStarts, totalFineSteps);
        final double gateRatio = role == NestedRhythmPattern.Role.RATCHET_INTERIOR ? 0.72 : 0.82;
        return Math.max(1, (int) Math.round(gap * gateRatio));
    }

    private int gapToNextDurationStart(final int fineStart,
                                       final List<Integer> durationStarts,
                                       final int totalFineSteps) {
        if (durationStarts.isEmpty()) {
            return totalFineSteps;
        }
        for (final int candidate : durationStarts) {
            if (candidate > fineStart) {
                return candidate - fineStart;
            }
        }
        return totalFineSteps - fineStart + durationStarts.get(0);
    }

    private List<Integer> rotateVelocities(final List<Integer> velocities, final int rotation) {
        if (velocities.isEmpty()) {
            return List.of();
        }
        final int normalizedRotation = Math.floorMod(rotation, velocities.size());
        if (normalizedRotation == 0) {
            return List.copyOf(velocities);
        }
        final List<Integer> rotated = new ArrayList<>(velocities.size());
        for (int index = 0; index < velocities.size(); index++) {
            rotated.add(velocities.get(Math.floorMod(index + normalizedRotation, velocities.size())));
        }
        return rotated;
    }

    private List<Integer> applyVelocityDepth(final List<Integer> rawVelocities,
                                             final double velocityDepth,
                                             final int velocityCenter) {
        if (rawVelocities.isEmpty()) {
            return List.of();
        }
        final double depthScale = Math.max(MIN_VELOCITY_DEPTH, Math.min(MAX_VELOCITY_DEPTH, velocityDepth));
        final int center = Math.max(1, Math.min(127, velocityCenter));
        final List<Integer> shaped = new ArrayList<>(rawVelocities.size());
        for (final int raw : rawVelocities) {
            final int velocity = center + (int) Math.round((raw - center) * depthScale);
            shaped.add(Math.max(1, Math.min(127, velocity)));
        }
        return shaped;
    }

    private int velocityFor(final PulseSpec pulse, final int order) {
        final int base = baseVelocityFor(velocityRoleFor(pulse), order);
        final double shaped = base * outerVelocityContour(pulse);
        return Math.max(1, Math.min(127, (int) Math.round(shaped)));
    }

    private NestedRhythmPattern.Role velocityRoleFor(final PulseSpec pulse) {
        if (pulse.contourIndex() <= 0 || pulse.contourCount() <= pulse.subdivisionCount()) {
            return pulse.role();
        }
        return switch (pulse.role()) {
            case RATCHET_LEAD -> NestedRhythmPattern.Role.RATCHET_INTERIOR;
            case TUPLET_LEAD -> NestedRhythmPattern.Role.TUPLET_INTERIOR;
            default -> pulse.role();
        };
    }

    private int baseVelocityFor(final NestedRhythmPattern.Role role, final int order) {
        final int contour = contourAt(order);
        final int peak = switch (role) {
            case PRIMARY_ANCHOR -> 118;
            case SECONDARY_ANCHOR -> 110;
            case TUPLET_LEAD -> 114;
            case TUPLET_INTERIOR -> 100;
            case RATCHET_LEAD -> 116;
            case RATCHET_INTERIOR -> 98;
            case PICKUP -> 104;
        };
        return Math.max(1, Math.min(127, peak + contour));
    }

    private double outerVelocityContour(final PulseSpec pulse) {
        final int count = Math.max(1, pulse.contourCount());
        final double roleInfluence = outerVelocityRoleInfluence(pulse);
        if (count <= 1 || roleInfluence <= 0.0) {
            return 1.0;
        }
        if (pulse.contourIndex() <= 0) {
            return 1.0 - roleInfluence * 0.03;
        }
        final double progress = pulse.contourIndex() / (double) count;
        final double ramp = Math.pow(progress, 1.8);
        final double target = 0.58 + ramp * 0.40;
        return 1.0 - roleInfluence * (1.0 - target);
    }

    private double outerVelocityRoleInfluence(final PulseSpec pulse) {
        return switch (velocityRoleFor(pulse)) {
            case PRIMARY_ANCHOR -> 0.10;
            case SECONDARY_ANCHOR -> 0.18;
            case TUPLET_LEAD -> 0.22;
            case TUPLET_INTERIOR -> 0.86;
            case RATCHET_LEAD -> 0.18;
            case RATCHET_INTERIOR -> 1.0;
            case PICKUP -> 0.70;
        };
    }

    public record Settings(int midiNote, double density, int tupletDivisions, int tupletTargets,
                           int tupletTargetPhase, int ratchetDivisions, int ratchetTargets, int ratchetTargetPhase,
                           double velocityDepth, int velocityCenter, int velocityRotation, int rhythmRotation,
                           double cluster, int meterNumerator, int meterDenominator, int barCount, double rate) {
        public Settings(final int midiNote, final double density, final int tupletDivisions, final int tupletTargets,
                        final int tupletTargetPhase, final int ratchetDivisions, final int ratchetTargets,
                        final int ratchetTargetPhase, final double velocityDepth, final int velocityCenter,
                        final int velocityRotation, final int rhythmRotation, final double cluster,
                        final int meterNumerator, final int meterDenominator, final int barCount) {
            this(midiNote, density, tupletDivisions, tupletTargets, tupletTargetPhase,
                    ratchetDivisions, ratchetTargets, ratchetTargetPhase,
                    velocityDepth, velocityCenter, velocityRotation, rhythmRotation, cluster, meterNumerator,
                    meterDenominator, barCount, 1.0);
        }

        public Settings(final int midiNote, final double density, final int tupletDivisions, final int tupletTargets,
                        final int tupletTargetPhase, final int ratchetDivisions, final int ratchetTargets,
                        final int ratchetTargetPhase, final double velocityDepth, final int velocityCenter,
                        final int velocityRotation, final int rhythmRotation, final int meterNumerator,
                        final int meterDenominator, final int barCount) {
            this(midiNote, density, tupletDivisions, tupletTargets, tupletTargetPhase,
                    ratchetDivisions, ratchetTargets, ratchetTargetPhase,
                    velocityDepth, velocityCenter, velocityRotation, rhythmRotation, 0.0, meterNumerator,
                    meterDenominator, barCount);
        }

        public Settings normalized() {
            final int normalizedBarCount = Math.max(1, Math.min(MAX_BARS, barCount));
            final int normalizedMeterNumerator = normalizeNumerator(meterNumerator);
            final int normalizedMeterDenominator = normalizeDenominator(meterDenominator);
            final int barFineSteps = fineStepsPerBar(normalizedMeterNumerator, normalizedMeterDenominator);
            final int totalFineSteps = barFineSteps * normalizedBarCount;
            final double normalizedRate = normalizeRate(rate);
            final int totalHalfBars = totalTupletSpans(totalFineSteps, tupletSpanFineSteps(barFineSteps, normalizedRate));
            final int normalizedTupletTargets = Math.max(0, Math.min(totalHalfBars, tupletTargets));
            final int normalizedTupletDivisions = normalizeCount(tupletDivisions, supportedTupletDivisions(
                    normalizedMeterNumerator,
                    normalizedMeterDenominator));
            final int normalizedTupletTargetPhase = Math.floorMod(tupletTargetPhase, totalHalfBars);
            final double normalizedCluster = Math.max(0.0, Math.min(1.0, cluster));
            final int ratchetParentRegionCount = ratchetParentRegionCount(
                    normalizedMeterNumerator,
                    normalizedMeterDenominator,
                    normalizedBarCount,
                    normalizedTupletDivisions,
                    normalizedTupletTargets,
                    normalizedTupletTargetPhase,
                    normalizedCluster,
                    normalizedRate);
            return new Settings(
                    Math.max(0, Math.min(127, midiNote)),
                    Math.max(0.0, Math.min(1.0, density)),
                    normalizedTupletDivisions,
                    normalizedTupletTargets,
                    normalizedTupletTargetPhase,
                    normalizeCount(ratchetDivisions, SUPPORTED_RATCHET_DIVISIONS),
                    Math.max(0, Math.min(ratchetParentRegionCount, ratchetTargets)),
                    Math.floorMod(ratchetTargetPhase, Math.max(1, ratchetParentRegionCount)),
                    Math.max(MIN_VELOCITY_DEPTH, Math.min(MAX_VELOCITY_DEPTH, velocityDepth)),
                    Math.max(1, Math.min(127, velocityCenter)),
                    Math.max(0, Math.min(VELOCITY_CONTOUR.length - 1, velocityRotation)),
                    Math.floorMod(rhythmRotation, 16),
                    normalizedCluster,
                    normalizedMeterNumerator,
                    normalizedMeterDenominator,
                    normalizedBarCount,
                    normalizedRate);
        }

        private int normalizeCount(final int count, final int[] supportedCounts) {
            for (final int supported : supportedCounts) {
                if (supported == count) {
                    return count;
                }
            }
            int best = supportedCounts[0];
            int bestDistance = Integer.MAX_VALUE;
            for (final int supported : supportedCounts) {
                final int distance = Math.abs(supported - count);
                if (distance < bestDistance || (distance == bestDistance && supported < best)) {
                    bestDistance = distance;
                    best = supported;
                }
            }
            return best;
        }
    }

    private static int normalizeNumerator(final int numerator) {
        return Math.max(1, Math.min(MAX_NUMERATOR, numerator));
    }

    private static double normalizeRate(final double rate) {
        double best = SUPPORTED_RATES[0];
        double bestDistance = Double.MAX_VALUE;
        for (final double supported : SUPPORTED_RATES) {
            final double distance = Math.abs(supported - rate);
            if (distance < bestDistance || (Math.abs(distance - bestDistance) <= 0.0001 && supported < best)) {
                bestDistance = distance;
                best = supported;
            }
        }
        return best;
    }

    private static int normalizeDenominator(final int denominator) {
        for (final int supported : SUPPORTED_DENOMINATORS) {
            if (supported == denominator) {
                return denominator;
            }
        }
        return DEFAULT_BEAT_DENOMINATOR;
    }

    private static int greatestCommonDivisor(final int left, final int right) {
        int a = Math.abs(left);
        int b = Math.abs(right);
        while (b != 0) {
            final int next = a % b;
            a = b;
            b = next;
        }
        return Math.max(1, a);
    }

    private record PulseSpec(int fineStart, NestedRhythmPattern.Role role, GestureFamily gestureFamily,
                             boolean required, int priority, int subdivisionIndex, int subdivisionCount,
                             int contourIndex, int contourCount) {
    }

    private record RatchetTargetRegion(int start, int length, GestureFamily gestureFamily) {
    }

    private record PulsePair(PulseSpec left, PulseSpec right) {
    }

    private enum GestureFamily {
        ANCHOR,
        RATCHET,
        TUPLET,
        NESTED_RATCHET,
        OTHER
    }

    private enum TargetOrderProfile {
        RATCHET,
        TUPLET,
        STRUCTURAL
    }
}
