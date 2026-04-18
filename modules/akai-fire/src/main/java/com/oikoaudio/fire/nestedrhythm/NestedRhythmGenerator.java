package com.oikoaudio.fire.nestedrhythm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NestedRhythmGenerator {
    public static final int BEATS_PER_BAR = 4;
    public static final int FINE_STEPS_PER_BEAT = 420;
    public static final int FINE_STEPS_PER_BAR = BEATS_PER_BAR * FINE_STEPS_PER_BEAT;
    private static final int[] SUPPORTED_COUNTS = {0, 3, 5, 7};
    private static final int[] CONNECTOR_PRIORITY = {
            315, 735, 1155, 1575,
            210, 630, 1050, 1470,
            105, 525, 945, 1365,
            53, 158, 263, 368,
            473, 578, 683, 788,
            893, 998, 1103, 1208,
            1313, 1418, 1523, 1628
    };
    private static final int[] VELOCITY_CONTOUR = {8, -3, 5, -6, 6, -2, 3, -5, 7, -4, 4, -3, 5, -6, 6, -2};

    public NestedRhythmPattern generate(final Settings settings) {
        final Settings normalized = settings.normalized();
        final TreeMap<Integer, NestedRhythmPattern.Role> structure = new TreeMap<>();
        addQuarterAnchors(structure);
        applyTuplet(structure, normalized);
        applyRatchet(structure, normalized);
        applyDensityConnectors(structure, normalized.density());
        final List<Map.Entry<Integer, NestedRhythmPattern.Role>> ordered = new ArrayList<>(structure.entrySet());
        ordered.sort(Map.Entry.comparingByKey());
        final List<NestedRhythmPattern.PulseEvent> events = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            final int fineStart = Math.floorMod(ordered.get(index).getKey()
                    + normalized.rhythmRotation() * sixteenthLength(), FINE_STEPS_PER_BAR);
            final NestedRhythmPattern.Role role = ordered.get(index).getValue();
            events.add(new NestedRhythmPattern.PulseEvent(
                    index,
                    fineStart,
                    0,
                    normalized.midiNote(),
                    velocityFor(role, index, normalized.velocityDepth(), normalized.velocityRotation()),
                    role));
        }
        events.sort(Comparator.comparingInt(NestedRhythmPattern.PulseEvent::fineStart));
        return new NestedRhythmPattern(withDurations(events));
    }

    private void addQuarterAnchors(final TreeMap<Integer, NestedRhythmPattern.Role> structure) {
        for (int beat = 0; beat < BEATS_PER_BAR; beat++) {
            structure.put(beat * FINE_STEPS_PER_BEAT, NestedRhythmPattern.Role.PRIMARY_ANCHOR);
        }
    }

    private void applyTuplet(final TreeMap<Integer, NestedRhythmPattern.Role> structure, final Settings settings) {
        if (settings.tupletCount() == 0 || settings.tupletCoverage() == TupletCoverage.NONE) {
            return;
        }
        final int start = settings.tupletCoverage() == TupletCoverage.BACK_HALF
                ? FINE_STEPS_PER_BAR / 2
                : 0;
        final int length = settings.tupletCoverage() == TupletCoverage.BACK_HALF
                ? FINE_STEPS_PER_BAR / 2
                : FINE_STEPS_PER_BAR;
        structure.subMap(start, true, start + length - 1, true).clear();
        final List<Integer> starts = evenlyDividedStarts(start, length, settings.tupletCount());
        for (int index = 0; index < starts.size(); index++) {
            structure.put(starts.get(index), index == 0
                    ? NestedRhythmPattern.Role.TUPLET_LEAD
                    : NestedRhythmPattern.Role.TUPLET_INTERIOR);
        }
    }

    private void applyRatchet(final TreeMap<Integer, NestedRhythmPattern.Role> structure, final Settings settings) {
        if (settings.ratchetCount() == 0) {
            return;
        }
        final int start = Math.max(0, Math.min(BEATS_PER_BAR - 1, settings.ratchetBeat())) * FINE_STEPS_PER_BEAT;
        structure.subMap(start, true, start + FINE_STEPS_PER_BEAT - 1, true).clear();
        final List<Integer> starts = evenlyDividedStarts(start, FINE_STEPS_PER_BEAT, settings.ratchetCount());
        for (int index = 0; index < starts.size(); index++) {
            structure.put(starts.get(index), index == 0
                    ? NestedRhythmPattern.Role.RATCHET_LEAD
                    : NestedRhythmPattern.Role.RATCHET_INTERIOR);
        }
    }

    private void applyDensityConnectors(final TreeMap<Integer, NestedRhythmPattern.Role> structure, final double density) {
        final int budget = Math.max(0, (int) Math.round(density * 8));
        int added = 0;
        for (final int candidate : CONNECTOR_PRIORITY) {
            if (added >= budget) {
                return;
            }
            if (!structure.containsKey(candidate)) {
                structure.put(candidate, added % 2 == 0
                        ? NestedRhythmPattern.Role.PICKUP
                        : NestedRhythmPattern.Role.SECONDARY_ANCHOR);
                added++;
            }
        }
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

    private int velocityFor(final NestedRhythmPattern.Role role, final int order,
                            final double velocityDepth, final int velocityRotation) {
        final int contour = VELOCITY_CONTOUR[Math.floorMod(order + velocityRotation, VELOCITY_CONTOUR.length)];
        final int floor = switch (role) {
            case PRIMARY_ANCHOR -> 96;
            case SECONDARY_ANCHOR -> 88;
            case TUPLET_LEAD -> 92;
            case TUPLET_INTERIOR -> 76;
            case RATCHET_LEAD -> 94;
            case RATCHET_INTERIOR -> 70;
            case PICKUP -> 82;
        };
        final int peak = switch (role) {
            case PRIMARY_ANCHOR -> 120;
            case SECONDARY_ANCHOR -> 108;
            case TUPLET_LEAD -> 112;
            case TUPLET_INTERIOR -> 92;
            case RATCHET_LEAD -> 115;
            case RATCHET_INTERIOR -> 88;
            case PICKUP -> 100;
        };
        final double shapedDepth = 0.35 + Math.max(0.0, Math.min(1.0, velocityDepth)) * 0.65;
        final int span = peak - floor;
        final int roleVelocity = floor + (int) Math.round(span * shapedDepth);
        return Math.max(1, Math.min(127, roleVelocity + contour));
    }

    private int sixteenthLength() {
        return FINE_STEPS_PER_BEAT / 4;
    }

    public enum TupletCoverage {
        NONE,
        BACK_HALF,
        FULL_BAR
    }

    public record Settings(int midiNote, double density, int tupletCount, TupletCoverage tupletCoverage,
                           int ratchetCount, int ratchetBeat, double velocityDepth,
                           int velocityRotation, int rhythmRotation) {
        public Settings normalized() {
            return new Settings(
                    Math.max(0, Math.min(127, midiNote)),
                    Math.max(0.0, Math.min(1.0, density)),
                    normalizeCount(tupletCount),
                    tupletCoverage == null ? TupletCoverage.NONE : tupletCoverage,
                    normalizeCount(ratchetCount),
                    Math.max(0, Math.min(BEATS_PER_BAR - 1, ratchetBeat)),
                    Math.max(0.0, Math.min(1.0, velocityDepth)),
                    Math.floorMod(velocityRotation, VELOCITY_CONTOUR.length),
                    Math.floorMod(rhythmRotation, 16));
        }

        private int normalizeCount(final int count) {
            for (final int supported : SUPPORTED_COUNTS) {
                if (supported == count) {
                    return count;
                }
            }
            return 0;
        }
    }
}
