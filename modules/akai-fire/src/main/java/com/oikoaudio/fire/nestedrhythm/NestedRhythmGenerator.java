package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;

public final class NestedRhythmGenerator {
    public static final int BEATS_PER_BAR = 4;
    public static final int FINE_STEPS_PER_BEAT = 420;
    public static final int FINE_STEPS_PER_BAR = BEATS_PER_BAR * FINE_STEPS_PER_BEAT;

    private static final int[] SUPPORTED_TUPLET_COUNTS = {0, 3, 5, 7};
    private static final int[] SUPPORTED_RATCHET_COUNTS = {0, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] RATCHET_REGION_ORDER = {0, 2, 1, 3};
    private static final int[] VELOCITY_CONTOUR = {18, -9, 12, -16, 14, -7, 9, -13, 16, -11, 10, -8, 13, -15, 15, -6};

    public NestedRhythmPattern generate(final Settings settings) {
        final Settings normalized = settings.normalized();
        final TreeMap<Integer, PulseSpec> structure = new TreeMap<>();
        addQuarterAnchors(structure);
        applyTuplet(structure, normalized);
        applyRatchet(structure, normalized);
        final List<PulseSpec> retained = thinStructure(structure, normalized.density());

        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(retained.size());
        final List<Integer> rawVelocities = new ArrayList<>(retained.size());
        retained.sort(Comparator.comparingInt(PulseSpec::fineStart));
        for (int index = 0; index < retained.size(); index++) {
            final PulseSpec pulse = retained.get(index);
            final int fineStart = Math.floorMod(pulse.fineStart()
                    + normalized.rhythmRotation() * sixteenthLength(), FINE_STEPS_PER_BAR);
            rawVelocities.add(velocityFor(pulse.role(), index));
            events.add(new NestedRhythmPattern.PulseEvent(
                    index,
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
        return new NestedRhythmPattern(withDurations(events));
    }

    private void addQuarterAnchors(final TreeMap<Integer, PulseSpec> structure) {
        for (int beat = 0; beat < BEATS_PER_BAR; beat++) {
            structure.put(beat * FINE_STEPS_PER_BEAT, new PulseSpec(
                    beat * FINE_STEPS_PER_BEAT,
                    NestedRhythmPattern.Role.PRIMARY_ANCHOR,
                    true,
                    1000));
        }
    }

    private void applyTuplet(final TreeMap<Integer, PulseSpec> structure, final Settings settings) {
        if (settings.tupletCount() == 0 || settings.tupletCoverage() == TupletCoverage.NONE) {
            return;
        }
        final int start;
        final int length;
        if (settings.tupletCoverage() == TupletCoverage.BACK_HALF) {
            length = FINE_STEPS_PER_BAR / 2;
            start = settings.tupletPhase() == 0 ? FINE_STEPS_PER_BAR / 2 : 0;
        } else {
            start = 0;
            length = FINE_STEPS_PER_BAR;
        }
        structure.subMap(start, true, start + length - 1, true).clear();
        final List<Integer> starts = evenlyDividedStarts(start, length, settings.tupletCount());
        addSubdivisionPulses(structure, starts, settings.tupletCount(),
                NestedRhythmPattern.Role.TUPLET_LEAD, NestedRhythmPattern.Role.TUPLET_INTERIOR, 900);
    }

    private void applyRatchet(final TreeMap<Integer, PulseSpec> structure, final Settings settings) {
        if (settings.ratchetCount() == 0) {
            return;
        }
        for (final int beat : ratchetedBeats(settings)) {
            final int start = beat * FINE_STEPS_PER_BEAT;
            structure.subMap(start, true, start + FINE_STEPS_PER_BEAT - 1, true).clear();
            final List<Integer> starts = evenlyDividedStarts(start, FINE_STEPS_PER_BEAT, settings.ratchetCount());
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
                    inserted == 0,
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
        return retained;
    }

    private List<Integer> ratchetedBeats(final Settings settings) {
        final int width = Math.max(1, Math.min(BEATS_PER_BAR, settings.ratchetWidth()));
        final int phase = Math.floorMod(settings.ratchetPhase(), BEATS_PER_BAR);
        final List<Integer> beats = new ArrayList<>(width);
        for (int index = 0; index < width; index++) {
            beats.add(RATCHET_REGION_ORDER[Math.floorMod(index + phase, RATCHET_REGION_ORDER.length)]);
        }
        return beats;
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

    private List<NestedRhythmPattern.PulseEvent> withDurations(final List<NestedRhythmPattern.PulseEvent> startsOnly) {
        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(startsOnly.size());
        for (int index = 0; index < startsOnly.size(); index++) {
            final NestedRhythmPattern.PulseEvent current = startsOnly.get(index);
            final NestedRhythmPattern.PulseEvent next = startsOnly.get((index + 1) % startsOnly.size());
            final int gap = index + 1 < startsOnly.size()
                    ? next.fineStart() - current.fineStart()
                    : FINE_STEPS_PER_BAR - current.fineStart() + next.fineStart();
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
        final int contour = VELOCITY_CONTOUR[Math.floorMod(order, VELOCITY_CONTOUR.length)];
        final int floor = switch (role) {
            case PRIMARY_ANCHOR -> 88;
            case SECONDARY_ANCHOR -> 82;
            case TUPLET_LEAD -> 84;
            case TUPLET_INTERIOR -> 66;
            case RATCHET_LEAD -> 86;
            case RATCHET_INTERIOR -> 62;
            case PICKUP -> 74;
        };
        final int peak = switch (role) {
            case PRIMARY_ANCHOR -> 118;
            case SECONDARY_ANCHOR -> 110;
            case TUPLET_LEAD -> 114;
            case TUPLET_INTERIOR -> 100;
            case RATCHET_LEAD -> 116;
            case RATCHET_INTERIOR -> 98;
            case PICKUP -> 104;
        };
        final int roleVelocity = peak;
        return Math.max(1, Math.min(127, roleVelocity + contour));
    }

    private int sixteenthLength() {
        return FINE_STEPS_PER_BEAT / 4;
    }

    public enum TupletCoverage {
        NONE,
        BACK_HALF,
        BOTH
    }

    public record Settings(int midiNote, double density, int tupletCount, TupletCoverage tupletCoverage,
                           int tupletPhase, int ratchetCount, int ratchetWidth, int ratchetPhase,
                           double velocityDepth, int velocityCenter, int velocityRotation, int rhythmRotation) {
        public Settings normalized() {
            return new Settings(
                    Math.max(0, Math.min(127, midiNote)),
                    Math.max(0.0, Math.min(1.0, density)),
                    normalizeCount(tupletCount, SUPPORTED_TUPLET_COUNTS),
                    tupletCoverage == null ? TupletCoverage.NONE : tupletCoverage,
                    Math.floorMod(tupletPhase, 2),
                    normalizeCount(ratchetCount, SUPPORTED_RATCHET_COUNTS),
                    Math.max(1, Math.min(BEATS_PER_BAR, ratchetWidth)),
                    Math.floorMod(ratchetPhase, BEATS_PER_BAR),
                    Math.max(0.25, Math.min(2.0, velocityDepth)),
                    Math.max(1, Math.min(127, velocityCenter)),
                    Math.max(0, Math.min(VELOCITY_CONTOUR.length - 1, velocityRotation)),
                    Math.floorMod(rhythmRotation, 16));
        }

        private int normalizeCount(final int count, final int[] supportedCounts) {
            for (final int supported : supportedCounts) {
                if (supported == count) {
                    return count;
                }
            }
            return 0;
        }
    }

    private record PulseSpec(int fineStart, NestedRhythmPattern.Role role, boolean required, int priority) {
    }
}
