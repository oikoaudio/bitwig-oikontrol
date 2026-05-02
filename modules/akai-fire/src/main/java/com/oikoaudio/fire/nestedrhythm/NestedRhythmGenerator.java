package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    private static final int[] SUPPORTED_DENOMINATORS = {2, 4, 8, 16};
    private static final int[] TUPLET_COUNT_CANDIDATES = {0, 3, 4, 5, 6, 7};
    private static final int[] SUPPORTED_RATCHET_DIVISIONS = {0, 2, 3, 4, 5, 6, 7, 8};
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
        final int target = windowStart + (int) Math.round(retainedIndex * windowLength / (double) retainedCount);
        final int blended = (int) Math.round(originalFineStart * (1.0 - cluster) + target * cluster);
        final int constrained = Math.max(windowStart, blended);
        final int remaining = retainedCount - retainedIndex - 1;
        final int minStart = previousStart + 1;
        final int maxStart = Math.max(minStart, totalFineSteps - remaining - 1);
        return snapClusteredFineStart(constrained, role, retainedCount, cluster, minStart, maxStart, totalFineSteps);
    }

    private int snapClusteredFineStart(final int desiredFineStart,
                                       final NestedRhythmPattern.Role role,
                                       final int retainedCount,
                                       final double cluster,
                                       final int minStart,
                                       final int maxStart,
                                       final int totalFineSteps) {
        final int divisions = clusterGridDivisions(role, retainedCount, cluster, totalFineSteps);
        final int candidate = nearestGridStart(desiredFineStart, divisions, minStart, maxStart, totalFineSteps);
        if (candidate >= 0) {
            return candidate;
        }
        return Math.max(minStart, Math.min(maxStart, desiredFineStart));
    }

    private int nearestGridStart(final int desiredFineStart,
                                 final int divisions,
                                 final int minStart,
                                 final int maxStart,
                                 final int totalFineSteps) {
        final int minIndex = Math.max(0, (int) Math.floor(
                minStart * divisions / (double) FINE_STEPS_PER_WHOLE) - 2);
        final int maxIndex = (int) Math.ceil(maxStart * divisions / (double) FINE_STEPS_PER_WHOLE) + 2;
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = minIndex; index <= maxIndex; index++) {
            final int candidate = (int) Math.round(index * FINE_STEPS_PER_WHOLE / (double) divisions);
            if (candidate < minStart || candidate > maxStart || candidate >= totalFineSteps) {
                continue;
            }
            final int distance = Math.abs(candidate - desiredFineStart);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best >= 0) {
            return best;
        }
        return -1;
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

    private TreeMap<Integer, PulseSpec> fullStructure(final Settings settings,
                                                      final int barFineSteps,
                                                      final int totalFineSteps) {
        final TreeMap<Integer, PulseSpec> structure = new TreeMap<>();
        addAnchors(structure, settings, barFineSteps);
        applyTuplet(structure, settings, barFineSteps, totalFineSteps);
        applyRatchet(structure, settings, barFineSteps, totalFineSteps);
        return structure;
    }

    private void addAnchors(final TreeMap<Integer, PulseSpec> structure,
                            final Settings settings,
                            final int barFineSteps) {
        final int beatFineSteps = fineStepsPerBeat(settings.meterDenominator());
        for (int barIndex = 0; barIndex < settings.barCount(); barIndex++) {
            final int barOffset = barIndex * barFineSteps;
            for (int beat = 0; beat < settings.meterNumerator(); beat++) {
                final int fineStart = Math.min(barOffset + beat * beatFineSteps,
                        barOffset + barFineSteps - 1);
                structure.put(fineStart, new PulseSpec(
                        fineStart,
                        NestedRhythmPattern.Role.PRIMARY_ANCHOR,
                        GestureFamily.ANCHOR,
                        true,
                        1000,
                        0,
                        1,
                        0,
                        1));
            }
        }
    }

    private void applyTuplet(final TreeMap<Integer, PulseSpec> structure,
                             final Settings settings,
                             final int barFineSteps,
                             final int totalFineSteps) {
        if (settings.tupletDivisions() == 0 || settings.tupletTargets() == 0) {
            return;
        }
        final int halfBarFineSteps = Math.max(1, barFineSteps / 2);
        final int totalHalfBars = settings.barCount() * 2;
        final List<Integer> selectedHalfBars = selectedTargetIndices(
                totalHalfBars, settings.tupletTargets(), settings.tupletTargetPhase());
        final int contourCount = Math.max(1, selectedHalfBars.size() * settings.tupletDivisions());
        int contourOffset = 0;
        for (final int halfBarIndex : selectedHalfBars) {
            final int start = halfBarIndex * halfBarFineSteps;
            if (settings.cluster() <= 0.0001) {
                clearWrappedSpan(structure, start, halfBarFineSteps, totalFineSteps);
            }
            final List<Integer> starts = evenlyDividedWrappedStarts(
                    start, halfBarFineSteps, settings.tupletDivisions(), totalFineSteps);
            addSubdivisionPulses(structure, starts, settings.tupletDivisions(), contourOffset, contourCount,
                    NestedRhythmPattern.Role.TUPLET_LEAD, NestedRhythmPattern.Role.TUPLET_INTERIOR,
                    GestureFamily.TUPLET);
            contourOffset += settings.tupletDivisions();
        }
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
        final int keepCount = optionalKeepCount(optional, density);
        if (cluster > 0.0001) {
            final List<PulseSpec> candidates = new ArrayList<>(structure.values());
            final int clusteredKeepCount = Math.max(0, Math.min(candidates.size(), required.size() + keepCount));
            return selectOptionalPulses(candidates, clusteredKeepCount, cluster, totalFineSteps);
        }
        final List<PulseSpec> retained = new ArrayList<>(required);
        retained.addAll(selectOptionalPulses(optional, keepCount, cluster, totalFineSteps));
        retained.sort(Comparator.comparingInt(PulseSpec::fineStart));
        return retained;
    }

    private int optionalKeepCount(final List<PulseSpec> optional, final double density) {
        return Math.max(0, Math.min(optional.size(), (int) Math.round(density * optional.size())));
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
        selected.sort(Comparator.comparingInt(PulseSpec::fineStart));
        return selected;
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
                    + compactPairBonus(left, right, totalFineSteps);
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
        return pulse.priority() + cluster * clusterMembership(pulse.fineStart(), cluster, totalFineSteps) * 900.0;
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
        final List<RatchetTargetRegion> ordered = ratchetTargetPriorityOrder(regions, settings.barCount());
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
                                                                 final int barCount) {
        if (barCount <= 1) {
            return regionPriorityOrder(regions);
        }
        final List<RatchetTargetRegion> ordered = new ArrayList<>(regions.size());
        for (final int index : phraseTargetPriorityOrder(regions.size())) {
            ordered.add(regions.get(index));
        }
        return ordered;
    }

    private List<RatchetTargetRegion> regionPriorityOrder(final List<RatchetTargetRegion> regions) {
        if (regions.isEmpty()) {
            return List.of();
        }
        final List<RatchetTargetRegion> ordered = new ArrayList<>(regions.size());
        for (final int index : targetPriorityOrder(regions.size())) {
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

    private static List<Integer> phraseTargetPriorityOrder(final int targetCount) {
        final List<Integer> ordered = new ArrayList<>(targetCount);
        addTargetIndex(ordered, targetCount, (int) Math.round(targetCount * 0.25));
        addTargetIndex(ordered, targetCount, targetCount / 2);
        addTargetIndex(ordered, targetCount, 0);
        addTargetIndex(ordered, targetCount, targetCount - 1);
        addTargetIndex(ordered, targetCount, 1);
        addTargetIndex(ordered, targetCount, targetCount - 2);
        for (int offset = 3; ordered.size() < targetCount; offset++) {
            addTargetIndex(ordered, targetCount, offset);
            addTargetIndex(ordered, targetCount, targetCount - offset);
        }
        return ordered;
    }

    private static void addTargetIndex(final List<Integer> ordered,
                                       final int targetCount,
                                       final int index) {
        if (index >= 0 && index < targetCount && !ordered.contains(index)) {
            ordered.add(index);
        }
    }

    private static List<Integer> selectedTargetIndices(final int totalTargets,
                                                       final int selectedCount,
                                                       final int targetPhase) {
        if (totalTargets <= 0 || selectedCount <= 0) {
            return List.of();
        }
        final List<Integer> ordered = targetPriorityOrder(totalTargets);
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
        final int normalizedMeterNumerator = normalizeNumerator(meterNumerator);
        final int normalizedMeterDenominator = normalizeDenominator(meterDenominator);
        final int normalizedBarCount = Math.max(1, Math.min(MAX_BARS, barCount));
        final int barFineSteps = fineStepsPerBar(normalizedMeterNumerator, normalizedMeterDenominator);
        final int totalFineSteps = barFineSteps * normalizedBarCount;
        final TreeMap<Integer, Boolean> starts = new TreeMap<>();
        final int beatFineSteps = fineStepsPerBeat(normalizedMeterDenominator);
        for (int barIndex = 0; barIndex < normalizedBarCount; barIndex++) {
            final int barOffset = barIndex * barFineSteps;
            for (int beat = 0; beat < normalizedMeterNumerator; beat++) {
                starts.put(Math.min(barOffset + beat * beatFineSteps, barOffset + barFineSteps - 1), true);
            }
        }
        if (tupletDivisions <= 0 || tupletTargets <= 0) {
            return starts.size();
        }
        final int halfBarFineSteps = Math.max(1, barFineSteps / 2);
        final int totalHalfBars = normalizedBarCount * 2;
        for (final int halfBarIndex : selectedTargetIndices(totalHalfBars, tupletTargets, tupletTargetPhase)) {
            final int start = halfBarIndex * halfBarFineSteps;
            if (cluster <= 0.0001) {
                starts.keySet().removeIf(candidate ->
                        isInsideWrappedSpan(candidate, start, halfBarFineSteps, totalFineSteps));
            }
            for (final int tupletStart : wrappedSubdivisionStarts(
                    start, halfBarFineSteps, tupletDivisions, totalFineSteps)) {
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
            final int duration = Math.min(
                    durationFor(current.fineStart(), current.role(), durationStarts, totalFineSteps),
                    Math.max(1, current.duration()));
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
                           double cluster, int meterNumerator, int meterDenominator, int barCount) {
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
            final int totalHalfBars = normalizedBarCount * 2;
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
                    normalizedCluster);
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
                    normalizedBarCount);
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
}
