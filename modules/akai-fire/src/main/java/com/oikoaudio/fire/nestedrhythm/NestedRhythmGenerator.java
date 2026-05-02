package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;

public final class NestedRhythmGenerator {
    public static final int DEFAULT_BEATS_PER_BAR = 4;
    public static final int DEFAULT_BEAT_DENOMINATOR = 4;
    public static final int FINE_STEPS_PER_QUARTER = 420;
    public static final int FINE_STEPS_PER_WHOLE = FINE_STEPS_PER_QUARTER * 4;
    public static final int BEATS_PER_BAR = DEFAULT_BEATS_PER_BAR;
    public static final int FINE_STEPS_PER_BEAT = FINE_STEPS_PER_QUARTER;
    public static final int FINE_STEPS_PER_BAR = FINE_STEPS_PER_WHOLE;

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
        final List<PulseSpec> retained = retainedStructure(normalized, barFineSteps, totalFineSteps);

        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(retained.size());
        final List<Integer> rawVelocities = new ArrayList<>(events.size());
        for (int order = 0; order < retained.size(); order++) {
            final PulseSpec pulse = retained.get(order);
            rawVelocities.add(velocityFor(pulse.role(), order));
            events.add(new NestedRhythmPattern.PulseEvent(
                    order,
                    pulse.fineStart(),
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
        return new NestedRhythmPattern(withDurations(events, totalFineSteps));
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

    private List<PulseSpec> retainedStructure(final Settings settings,
                                              final int barFineSteps,
                                              final int totalFineSteps) {
        final TreeMap<Integer, PulseSpec> structure = new TreeMap<>();
        addAnchors(structure, settings, barFineSteps);
        applyTuplet(structure, settings, barFineSteps, totalFineSteps);
        applyRatchet(structure, settings, barFineSteps, totalFineSteps);
        return thinStructure(structure, settings.density());
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
                        1000));
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
                NestedRhythmPattern.Role.TUPLET_LEAD, NestedRhythmPattern.Role.TUPLET_INTERIOR, 900);
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
                    NestedRhythmPattern.Role.RATCHET_LEAD, NestedRhythmPattern.Role.RATCHET_INTERIOR, 950);
        }
    }

    private void addSubdivisionPulses(final TreeMap<Integer, PulseSpec> structure,
                                      final List<Integer> starts,
                                      final int count,
                                      final NestedRhythmPattern.Role leadRole,
                                      final NestedRhythmPattern.Role interiorRole,
                                      final int basePriority) {
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
                    subdivisionPriority(index, count, basePriority)));
            inserted++;
        }
    }

    private List<PulseSpec> thinStructure(final TreeMap<Integer, PulseSpec> structure, final double density) {
        final List<PulseSpec> required = new ArrayList<>();
        final List<PulseSpec> optional = new ArrayList<>();
        for (final PulseSpec pulse : structure.values()) {
            if (pulse.required()) {
                required.add(pulse);
            } else {
                optional.add(pulse);
            }
        }
        optional.sort(Comparator.comparingInt(PulseSpec::priority).reversed()
                .thenComparingInt(PulseSpec::fineStart));
        final int keepCount = Math.max(0, Math.min(optional.size(), (int) Math.round(density * optional.size())));
        final List<PulseSpec> retained = new ArrayList<>(required);
        retained.addAll(optional.subList(0, keepCount));
        retained.sort(Comparator.comparingInt(PulseSpec::fineStart));
        return retained;
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

    private int subdivisionPriority(final int index, final int count, final int base) {
        if (index == 0) {
            return base;
        }
        final int mirroredDistance = Math.min(index, count - 1 - index);
        final int edgeBias = index == count - 1 ? 24 : 0;
        return base - mirroredDistance * 10 + edgeBias - index;
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
                                                               final int totalFineSteps) {
        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(startsOnly.size());
        for (int index = 0; index < startsOnly.size(); index++) {
            final NestedRhythmPattern.PulseEvent current = startsOnly.get(index);
            final NestedRhythmPattern.PulseEvent next = startsOnly.get((index + 1) % startsOnly.size());
            final int gap = index + 1 < startsOnly.size()
                    ? next.fineStart() - current.fineStart()
                    : totalFineSteps - current.fineStart() + next.fineStart();
            final double gateRatio = current.role() == NestedRhythmPattern.Role.RATCHET_INTERIOR ? 0.72 : 0.82;
            final int duration = Math.max(1, (int) Math.round(gap * gateRatio));
            events.add(new NestedRhythmPattern.PulseEvent(current.order(), current.fineStart(),
                    duration, current.midiNote(), current.velocity(), current.role()));
        }
        return events;
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
        final double depthScale = Math.max(0.25, Math.min(2.0, velocityDepth));
        final int center = Math.max(1, Math.min(127, velocityCenter));
        final List<Integer> shaped = new ArrayList<>(rawVelocities.size());
        for (final int raw : rawVelocities) {
            final int velocity = center + (int) Math.round((raw - center) * depthScale);
            shaped.add(Math.max(1, Math.min(127, velocity)));
        }
        return shaped;
    }

    private int velocityFor(final NestedRhythmPattern.Role role, final int order) {
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

    public record Settings(int midiNote, double density, int tupletCount, int tupletCover,
                           int tupletPhase, int ratchetCount, int ratchetWidth, int ratchetPhase,
                           double velocityDepth, int velocityCenter, int velocityRotation, int rhythmRotation,
                           int meterNumerator, int meterDenominator, int barCount) {
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
                    Math.max(0.25, Math.min(2.0, velocityDepth)),
                    Math.max(1, Math.min(127, velocityCenter)),
                    Math.max(0, Math.min(VELOCITY_CONTOUR.length - 1, velocityRotation)),
                    Math.floorMod(rhythmRotation, 16),
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

    private record PulseSpec(int fineStart, NestedRhythmPattern.Role role, boolean required, int priority) {
    }
}
