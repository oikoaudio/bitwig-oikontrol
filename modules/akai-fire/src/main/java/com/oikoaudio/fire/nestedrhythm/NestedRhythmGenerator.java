package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
    private static final int[] SUPPORTED_RATCHET_COUNTS = {0, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] VELOCITY_CONTOUR = {18, -9, 12, -16, 14, -7, 9, -13, 16, -11, 10, -8, 13, -15, 15, -6};

    public NestedRhythmPattern generate(final Settings settings) {
        final Settings normalized = settings.normalized();
        final int barFineSteps = fineStepsPerBar(normalized.meterNumerator(), normalized.meterDenominator());
        final int totalFineSteps = barFineSteps * normalized.barCount();
        final TreeMap<Integer, PulseSpec> structure = fullStructure(normalized, barFineSteps, totalFineSteps);
        final List<PulseSpec> retained = thinStructure(structure, normalized.density(),
                normalized.cluster(), totalFineSteps);
        final List<Integer> durationStarts = List.copyOf(structure.keySet());

        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(retained.size());
        final List<Integer> rawVelocities = new ArrayList<>(events.size());
        int lastClusteredStart = -1;
        for (int order = 0; order < retained.size(); order++) {
            final PulseSpec pulse = retained.get(order);
            final int structureOrder = durationStarts.indexOf(pulse.fineStart());
            final int fineStart = clusteredFineStart(pulse.fineStart(), order, retained.size(),
                    normalized.cluster(), totalFineSteps, lastClusteredStart);
            lastClusteredStart = fineStart;
            rawVelocities.add(velocityFor(pulse, structureOrder));
            events.add(new NestedRhythmPattern.PulseEvent(
                    structureOrder,
                    fineStart,
                    0,
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
                                   final int previousStart) {
        if (cluster <= 0.0001 || retainedCount <= 0 || totalFineSteps <= 1) {
            return originalFineStart;
        }
        final int windowLength = clusterWindowLength(cluster, totalFineSteps);
        final int windowStart = Math.max(0, totalFineSteps - windowLength);
        final int target = retainedCount == 1
                ? windowStart
                : windowStart + (int) Math.round(retainedIndex * (windowLength - 1) / (double) (retainedCount - 1));
        final int blended = (int) Math.round(originalFineStart * (1.0 - cluster) + target * cluster);
        final int remaining = retainedCount - retainedIndex - 1;
        final int minStart = previousStart + 1;
        final int maxStart = Math.max(minStart, totalFineSteps - remaining - 1);
        return Math.max(minStart, Math.min(maxStart, blended));
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

    public static int ratchetRegionAt(final int beatCount, final int orderedIndex) {
        final int clampedBeatCount = Math.max(1, beatCount);
        return Math.floorMod(orderedIndex, clampedBeatCount);
    }

    public static int[] supportedTupletCounts(final int meterNumerator,
                                              final int meterDenominator,
                                              final int tupletCover) {
        final int normalizedCover = Math.max(1, tupletCover);
        final int barFineSteps = fineStepsPerBar(meterNumerator, meterDenominator);
        final int halfBarFineSteps = Math.max(1, barFineSteps / 2);
        final int spanFineSteps = halfBarFineSteps * normalizedCover;
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
                        true,
                        1000,
                        0,
                        1));
            }
        }
    }

    private void applyTuplet(final TreeMap<Integer, PulseSpec> structure,
                             final Settings settings,
                             final int barFineSteps,
                             final int totalFineSteps) {
        if (settings.tupletCount() == 0 || settings.tupletCover() == 0) {
            return;
        }
        final int halfBarFineSteps = Math.max(1, barFineSteps / 2);
        final int totalHalfBars = settings.barCount() * 2;
        final int normalizedCover = Math.max(0, Math.min(totalHalfBars, settings.tupletCover()));
        final int startHalfBar = Math.floorMod(settings.tupletPhase(), totalHalfBars);
        final int start = startHalfBar * halfBarFineSteps;
        final int length = normalizedCover * halfBarFineSteps;

        clearWrappedSpan(structure, start, length, totalFineSteps);
        final List<Integer> starts = evenlyDividedWrappedStarts(start, length, settings.tupletCount(), totalFineSteps);
        addSubdivisionPulses(structure, starts, settings.tupletCount(),
                NestedRhythmPattern.Role.TUPLET_LEAD, NestedRhythmPattern.Role.TUPLET_INTERIOR);
    }

    private void applyRatchet(final TreeMap<Integer, PulseSpec> structure,
                              final Settings settings,
                              final int barFineSteps,
                              final int totalFineSteps) {
        if (settings.ratchetCount() == 0) {
            return;
        }
        final int beatFineSteps = fineStepsPerBeat(settings.meterDenominator());
        for (final int beatIndex : ratchetedBeats(settings)) {
            final int start = Math.min(beatIndex * beatFineSteps, totalFineSteps - 1);
            structure.subMap(start, true, Math.min(totalFineSteps - 1, start + beatFineSteps - 1), true).clear();
            final List<Integer> starts = evenlyDividedStarts(start, beatFineSteps, settings.ratchetCount());
            addSubdivisionPulses(structure, starts, settings.ratchetCount(),
                    NestedRhythmPattern.Role.RATCHET_LEAD, NestedRhythmPattern.Role.RATCHET_INTERIOR);
        }
    }

    private void addSubdivisionPulses(final TreeMap<Integer, PulseSpec> structure,
                                      final List<Integer> starts,
                                      final int count,
                                      final NestedRhythmPattern.Role leadRole,
                                      final NestedRhythmPattern.Role interiorRole) {
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
                    false,
                    subdivisionPriority(role, index, count),
                    index,
                    count));
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

        while (ranked.size() + 1 < optional.size()) {
            final PulsePair pair = bestOptionalPair(timeOrdered, selectedStarts, cluster, totalFineSteps);
            if (pair == null) {
                break;
            }
            ranked.add(pair.left());
            ranked.add(pair.right());
            selectedStarts.add(pair.left().fineStart());
            selectedStarts.add(pair.right().fineStart());
        }

        while (ranked.size() < optional.size()) {
            final PulseSpec pulse = bestOptionalSingle(timeOrdered, selectedStarts, cluster, totalFineSteps);
            if (pulse == null) {
                break;
            }
            ranked.add(pulse);
            selectedStarts.add(pulse.fineStart());
        }
        return ranked;
    }

    private PulsePair bestOptionalPair(final List<PulseSpec> optional,
                                       final Set<Integer> selectedStarts,
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
            if (!samePriorityFamily(left.role(), right.role())) {
                continue;
            }
            final double score = optionalScore(left, cluster, totalFineSteps)
                    + optionalScore(right, cluster, totalFineSteps)
                    + 75.0;
            if (score > bestScore) {
                bestScore = score;
                best = new PulsePair(left, right);
            }
        }
        return best;
    }

    private boolean samePriorityFamily(final NestedRhythmPattern.Role left,
                                       final NestedRhythmPattern.Role right) {
        return priorityFamily(left) == priorityFamily(right);
    }

    private PriorityFamily priorityFamily(final NestedRhythmPattern.Role role) {
        return switch (role) {
            case RATCHET_LEAD, RATCHET_INTERIOR -> PriorityFamily.RATCHET;
            case TUPLET_LEAD, TUPLET_INTERIOR -> PriorityFamily.TUPLET;
            default -> PriorityFamily.OTHER;
        };
    }

    private PulseSpec bestOptionalSingle(final List<PulseSpec> optional,
                                         final Set<Integer> selectedStarts,
                                         final double cluster,
                                         final int totalFineSteps) {
        PulseSpec best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (final PulseSpec pulse : optional) {
            if (selectedStarts.contains(pulse.fineStart())) {
                continue;
            }
            final double score = optionalScore(pulse, cluster, totalFineSteps);
            if (score > bestScore) {
                bestScore = score;
                best = pulse;
            }
        }
        return best;
    }

    private double optionalScore(final PulseSpec pulse,
                                 final double cluster,
                                 final int totalFineSteps) {
        return pulse.priority() + cluster * clusterMembership(pulse.fineStart(), cluster, totalFineSteps) * 900.0;
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
        final double windowFraction = 1.0 - Math.min(1.0, Math.max(0.0, cluster)) * 0.75;
        return Math.max(1, (int) Math.round(totalFineSteps * windowFraction));
    }

    private List<Integer> ratchetedBeats(final Settings settings) {
        final int beatCount = settings.meterNumerator();
        final int totalBeatCount = beatCount * settings.barCount();
        final int width = Math.max(1, Math.min(totalBeatCount, settings.ratchetWidth()));
        final int phase = Math.floorMod(settings.ratchetPhase(), totalBeatCount);
        final List<Integer> beats = new ArrayList<>(width);
        for (int index = 0; index < width; index++) {
            final int baseRegion = phraseRatchetRegionAt(beatCount, settings.barCount(), index);
            beats.add(Math.floorMod(baseRegion + phase, totalBeatCount));
        }
        return beats;
    }

    public static int phraseRatchetRegionAt(final int beatCount, final int barCount, final int orderedIndex) {
        final int clampedBeatCount = Math.max(1, beatCount);
        final int clampedBarCount = Math.max(1, barCount);
        final int totalRegions = clampedBeatCount * clampedBarCount;
        final int normalizedIndex = Math.floorMod(orderedIndex, totalRegions);
        final int beatPriorityIndex = normalizedIndex / clampedBarCount;
        final int barIndex = normalizedIndex % clampedBarCount;
        return barIndex * clampedBeatCount + ratchetRegionAt(clampedBeatCount, beatPriorityIndex);
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
            final int gap = gapToNextDurationStart(current.fineStart(), durationStarts, totalFineSteps);
            final double gateRatio = current.role() == NestedRhythmPattern.Role.RATCHET_INTERIOR ? 0.72 : 0.82;
            final int duration = Math.max(1, (int) Math.round(gap * gateRatio));
            events.add(new NestedRhythmPattern.PulseEvent(current.order(), current.fineStart(),
                    duration, current.midiNote(), current.velocity(), current.role()));
        }
        return events;
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
        final int base = baseVelocityFor(pulse.role(), order);
        final double shaped = base * outerVelocityContour(pulse);
        return Math.max(1, Math.min(127, (int) Math.round(shaped)));
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
        final int count = Math.max(1, pulse.subdivisionCount());
        final double roleInfluence = outerVelocityRoleInfluence(pulse.role());
        if (count <= 1 || roleInfluence <= 0.0) {
            return 1.0;
        }
        if (pulse.subdivisionIndex() <= 0) {
            return 1.0 - roleInfluence * 0.03;
        }
        final double progress = pulse.subdivisionIndex() / (double) count;
        final double ramp = Math.pow(progress, 1.8);
        final double target = 0.58 + ramp * 0.40;
        return 1.0 - roleInfluence * (1.0 - target);
    }

    private double outerVelocityRoleInfluence(final NestedRhythmPattern.Role role) {
        return switch (role) {
            case PRIMARY_ANCHOR -> 0.10;
            case SECONDARY_ANCHOR -> 0.18;
            case TUPLET_LEAD -> 0.22;
            case TUPLET_INTERIOR -> 0.86;
            case RATCHET_LEAD -> 0.18;
            case RATCHET_INTERIOR -> 1.0;
            case PICKUP -> 0.70;
        };
    }

    public record Settings(int midiNote, double density, int tupletCount, int tupletCover,
                           int tupletPhase, int ratchetCount, int ratchetWidth, int ratchetPhase,
                           double velocityDepth, int velocityCenter, int velocityRotation, int rhythmRotation,
                           double cluster, int meterNumerator, int meterDenominator, int barCount) {
        public Settings(final int midiNote, final double density, final int tupletCount, final int tupletCover,
                        final int tupletPhase, final int ratchetCount, final int ratchetWidth,
                        final int ratchetPhase, final double velocityDepth, final int velocityCenter,
                        final int velocityRotation, final int rhythmRotation, final int meterNumerator,
                        final int meterDenominator, final int barCount) {
            this(midiNote, density, tupletCount, tupletCover, tupletPhase, ratchetCount, ratchetWidth, ratchetPhase,
                    velocityDepth, velocityCenter, velocityRotation, rhythmRotation, 0.0, meterNumerator,
                    meterDenominator, barCount);
        }

        public Settings normalized() {
            final int normalizedBarCount = Math.max(1, Math.min(MAX_BARS, barCount));
            final int normalizedMeterNumerator = normalizeNumerator(meterNumerator);
            final int normalizedMeterDenominator = normalizeDenominator(meterDenominator);
            final int totalHalfBars = normalizedBarCount * 2;
            return new Settings(
                    Math.max(0, Math.min(127, midiNote)),
                    Math.max(0.0, Math.min(1.0, density)),
                    normalizeCount(tupletCount, supportedTupletCounts(
                            normalizedMeterNumerator,
                            normalizedMeterDenominator,
                            Math.max(1, Math.min(totalHalfBars, tupletCover)))),
                    Math.max(0, Math.min(totalHalfBars, tupletCover)),
                    Math.floorMod(tupletPhase, totalHalfBars),
                    normalizeCount(ratchetCount, SUPPORTED_RATCHET_COUNTS),
                    Math.max(1, Math.min(normalizedMeterNumerator * normalizedBarCount,
                            ratchetWidth)),
                    Math.floorMod(ratchetPhase, Math.max(1, normalizedMeterNumerator * normalizedBarCount)),
                    Math.max(MIN_VELOCITY_DEPTH, Math.min(MAX_VELOCITY_DEPTH, velocityDepth)),
                    Math.max(1, Math.min(127, velocityCenter)),
                    Math.max(0, Math.min(VELOCITY_CONTOUR.length - 1, velocityRotation)),
                    Math.floorMod(rhythmRotation, 16),
                    Math.max(0.0, Math.min(1.0, cluster)),
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

    private record PulseSpec(int fineStart, NestedRhythmPattern.Role role, boolean required, int priority,
                             int subdivisionIndex, int subdivisionCount) {
    }

    private record PulsePair(PulseSpec left, PulseSpec right) {
    }

    private enum PriorityFamily {
        RATCHET,
        TUPLET,
        OTHER
    }
}
